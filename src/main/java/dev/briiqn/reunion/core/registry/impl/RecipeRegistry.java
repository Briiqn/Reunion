/*
 * Copyright (C) 2026 Briiqn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.briiqn.reunion.core.registry.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.data.Recipe;
import dev.briiqn.reunion.core.registry.Registry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class RecipeRegistry extends Registry<Recipe> {

  private static volatile RecipeRegistry INSTANCE;
  private final List<Recipe> recipes;
  private final Int2ObjectMap<List<Recipe>> byResultId;

  private RecipeRegistry(List<Recipe> recipes, Int2ObjectMap<List<Recipe>> byResultId) {
    this.recipes = Collections.unmodifiableList(recipes);
    this.byResultId = Int2ObjectMaps.unmodifiable(byResultId);
  }

  public static RecipeRegistry getInstance() {
    if (INSTANCE == null) {
      synchronized (RecipeRegistry.class) {
        if (INSTANCE == null) {
          INSTANCE = load();
        }
      }
    }
    return INSTANCE;
  }

  private static RecipeRegistry load() {
    try {
      String jsonText;
      try (InputStream in = RecipeRegistry.class.getResourceAsStream("/recipes.json")) {
        if (in == null) {
          throw new RuntimeException("recipes.json not found");
        }
        jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }

      Object parsed = JSON.parse(jsonText);

      List<Recipe> recipes = new ArrayList<>();
      Int2ObjectOpenHashMap<List<Recipe>> byResultId = new Int2ObjectOpenHashMap<>();

      if (parsed instanceof JSONArray rootArray) {
        for (int i = 0; i < rootArray.size(); i++) {
          parseAndAdd(rootArray.getJSONObject(i), recipes, byResultId, -1);
        }
      } else if (parsed instanceof JSONObject rootObj) {
        log.warn(
            "[RecipeRegistry] recipes.json is grouped by ID. This will cause index mismatches with the client!");

        List<Integer> keys = new ArrayList<>();
        for (String key : rootObj.keySet()) {
          keys.add(Integer.parseInt(key));
        }
        Collections.sort(keys);

        for (int resultIdKey : keys) {
          JSONArray arr = rootObj.getJSONArray(String.valueOf(resultIdKey));
          for (int i = 0; i < arr.size(); i++) {
            parseAndAdd(arr.getJSONObject(i), recipes, byResultId, resultIdKey);
          }
        }
      }

      byResultId.replaceAll((k, v) -> Collections.unmodifiableList(v));

      log.info("[RecipeRegistry] Loaded " + recipes.size() + " recipes (" + byResultId.size()
          + " unique result IDs)");
      return new RecipeRegistry(recipes, byResultId);

    } catch (Exception e) {
      log.error("[RecipeRegistry] Failed to load recipe data", e);
      return new RecipeRegistry(Collections.emptyList(), new Int2ObjectOpenHashMap<>());
    }
  }

  private static void parseAndAdd(JSONObject obj, List<Recipe> recipes,
      Int2ObjectOpenHashMap<List<Recipe>> byResultId, int fallbackResultId) {
    JSONObject resultObj = obj.getJSONObject("result");
    int resultId = resultObj != null && resultObj.containsKey("id") ? resultObj.getIntValue("id")
        : fallbackResultId;
    int resultMeta =
        resultObj != null && resultObj.containsKey("metadata") ? resultObj.getIntValue("metadata")
            : 0;
    int resultCount =
        resultObj != null && resultObj.containsKey("count") ? resultObj.getIntValue("count") : 1;

    Recipe recipe = obj.containsKey("inShape")
        ? parseShaped(resultId, resultMeta, resultCount, obj.getJSONArray("inShape"))
        : parseShapeless(resultId, resultMeta, resultCount, obj.getJSONArray("ingredients"));

    recipes.add(recipe);
    byResultId.computeIfAbsent(resultId, k -> new ArrayList<>()).add(recipe);
  }

  private static Recipe parseShaped(int resultId, int resultMeta, int resultCount,
      JSONArray shapeArray) {
    int rows = shapeArray.size();
    int cols = rows == 0 ? 0 : shapeArray.getJSONArray(0).size();

    int[][] ids = new int[rows][cols];
    int[][] meta = new int[rows][cols];

    for (int r = 0; r < rows; r++) {
      JSONArray row = shapeArray.getJSONArray(r);
      for (int c = 0; c < row.size(); c++) {
        Object cell = row.get(c);
        if (cell instanceof JSONObject ingredient) {
          ids[r][c] = ingredient.getIntValue("id");
          meta[r][c] = ingredient.containsKey("metadata") ? ingredient.getIntValue("metadata") : -1;
        } else if (cell instanceof Number n) {
          ids[r][c] = n.intValue();
          meta[r][c] = -1;
        } else {
          ids[r][c] = 0;
          meta[r][c] = -1;
        }
      }
    }
    return new Recipe(resultId, resultMeta, resultCount, ids, meta);
  }

  private static Recipe parseShapeless(int resultId, int resultMeta, int resultCount,
      JSONArray ingredientArray) {
    int count = ingredientArray == null ? 0 : ingredientArray.size();
    int[] ids = new int[count];
    int[] meta = new int[count];

    for (int i = 0; i < count; i++) {
      Object cell = ingredientArray.get(i);
      if (cell instanceof JSONObject ingredient) {
        ids[i] = ingredient.getIntValue("id");
        meta[i] = ingredient.containsKey("metadata") ? ingredient.getIntValue("metadata") : -1;
      } else if (cell instanceof Number n) {
        ids[i] = n.intValue();
        meta[i] = -1;
      } else {
        ids[i] = 0;
        meta[i] = -1;
      }
    }
    return new Recipe(resultId, resultMeta, resultCount, ids, meta);
  }

  @Override
  public Recipe get(int index) {
    if (index < 0 || index >= recipes.size()) {
      return null;
    }
    return recipes.get(index);
  }

  @Override
  public boolean isUnsupported(int index) {
    Recipe r = get(index);
    if (r == null) {
      return true;
    }
    int id = r.resultId();
    return id < 256
        ? BlockRegistry.getInstance().isUnsupported(id)
        : ItemRegistry.getInstance().isUnsupported(id);
  }

  @Override
  public int remap(int index) {
    Recipe r = get(index);
    if (r == null) {
      return -1;
    }
    int id = r.resultId();
    return id < 256
        ? BlockRegistry.getInstance().remap(id)
        : ItemRegistry.getInstance().remap(id);
  }

  public int size() {
    return recipes.size();
  }

  public List<Recipe> findByResult(int itemId) {
    return byResultId.getOrDefault(itemId, Collections.emptyList());
  }
}