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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.data.Item;
import dev.briiqn.reunion.core.registry.ArrayRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ItemRegistry extends ArrayRegistry<Item> {

  private static final Set<String> UNSUPPORTED_ITEM_NAMES = Set.of(
      "writable_book", "written_book", "nether_star",
      "fireworks", "firework_charge", "comparator"
  );
  private static final int UNSUPPORTED_RANGE_START = 407;
  private static final int UNSUPPORTED_RANGE_END = 2255;
  private static volatile ItemRegistry INSTANCE;
  private final Map<Integer, FoodInfo> foodsById;
  private final Map<Integer, Integer> remapTable;

  private ItemRegistry(Item[] entries, Map<Integer, FoodInfo> foodsById,
      Map<Integer, Integer> remapTable) {
    super(entries);
    this.foodsById = Collections.unmodifiableMap(foodsById);
    this.remapTable = Collections.unmodifiableMap(remapTable);
  }

  public static ItemRegistry getInstance() {
    if (INSTANCE == null) {
      synchronized (ItemRegistry.class) {
        if (INSTANCE == null) {
          INSTANCE = load();
        }
      }
    }
    return INSTANCE;
  }

  private static int computeFallback(int id, int meta, Item[] entries,
      Map<Integer, FoodInfo> foodsById) {
    return foodsById.containsKey(id)
        ? computeFoodFallback(id, entries, foodsById)
        : computeNameFallback(id, meta, entries, foodsById);
  }

  private static int computeFoodFallback(int id, Item[] entries, Map<Integer, FoodInfo> foodsById) {
    FoodInfo original = foodsById.get(id);
    if (original == null) {
      return computeNameFallback(id, 0, entries, foodsById);
    }

    boolean isBlock = id > 0 && id < 256;
    int bestId = isBlock ? 103 : 260;
    int bestPointDiff = Integer.MAX_VALUE;
    float bestSatDiff = Float.MAX_VALUE;

    for (FoodInfo candidate : foodsById.values()) {
      if (isUnsupportedStatic(candidate.id(), entries)) {
        continue;
      }
      if ((candidate.id() > 0 && candidate.id() < 256) != isBlock) {
        continue;
      }

      int pointDiff = Math.abs(candidate.foodPoints() - original.foodPoints());
      float satDiff = Math.abs(candidate.saturation() - original.saturation());

      if (pointDiff < bestPointDiff || (pointDiff == bestPointDiff && satDiff < bestSatDiff)) {
        bestPointDiff = pointDiff;
        bestSatDiff = satDiff;
        bestId = candidate.id();
      }
    }
    return bestId;
  }

  private static int computeNameFallback(int id, int meta, Item[] entries,
      Map<Integer, FoodInfo> foodsById) {
    Item original = id >= 0 && id < entries.length ? entries[id] : null;
    String targetName = original != null
        ? original.displayNameFor(meta).toLowerCase(Locale.ROOT)
        : ("item " + id);

    boolean isBlock = id > 0 && id < 256;
    int bestId = isBlock ? 1 : 339;
    int bestDist = Integer.MAX_VALUE;

    for (Item candidate : entries) {
      if (candidate == null || isUnsupportedStatic(candidate.id(), entries)) {
        continue;
      }
      if ((candidate.id() > 0 && candidate.id() < 256) != isBlock) {
        continue;
      }

      int dist = BlockRegistry.editDistance(
          targetName, candidate.displayName().toLowerCase(Locale.ROOT));
      if (dist < bestDist) {
        bestDist = dist;
        bestId = candidate.id();
      }
    }
    return bestId;
  }

  private static boolean isUnsupportedStatic(int id, Item[] entries) {
    Item item = id >= 0 && id < entries.length ? entries[id] : null;
    if (item == null) {
      return id > 0;
    }
    if (UNSUPPORTED_ITEM_NAMES.contains(item.name())) {
      return true;
    }
    return id >= UNSUPPORTED_RANGE_START && id <= UNSUPPORTED_RANGE_END;
  }

  private static ItemRegistry load() {
    try {
      JSONArray itemsJson = readJsonArray(ItemRegistry.class, "/items.json");
      JSONArray foodsJson = readJsonArray(ItemRegistry.class, "/foods.json");

      int maxId = 0;
      for (int i = 0; i < itemsJson.size(); i++) {
        int id = itemsJson.getJSONObject(i).getIntValue("id");
        if (id > maxId) {
          maxId = id;
        }
      }

      Item[] byId = new Item[maxId + 1];
      for (int i = 0; i < itemsJson.size(); i++) {
        JSONObject obj = itemsJson.getJSONObject(i);
        int id = obj.getIntValue("id");
        String name = obj.getString("name");
        String displayName = obj.getString("displayName");
        int stackSize = obj.getIntValue("stackSize");

        Map<Integer, String> variations = new LinkedHashMap<>();
        JSONArray varArray = obj.getJSONArray("variations");
        if (varArray != null) {
          for (int v = 0; v < varArray.size(); v++) {
            JSONObject var = varArray.getJSONObject(v);
            variations.put(var.getIntValue("metadata"), var.getString("displayName"));
          }
        }
        byId[id] = new Item(id, name, displayName, stackSize, variations);
      }

      Map<Integer, FoodInfo> foods = new HashMap<>();
      for (int i = 0; i < foodsJson.size(); i++) {
        JSONObject obj = foodsJson.getJSONObject(i);
        int id = obj.getIntValue("id");
        int foodPoints = obj.getIntValue("foodPoints");
        float saturation = obj.getFloatValue("saturation");
        foods.put(id, new FoodInfo(id, foodPoints, saturation));
      }

      Map<Integer, Integer> remapTable = new HashMap<>();
      for (int id = 0; id < byId.length; id++) {
        if (byId[id] == null) {
          continue;
        }
        remapTable.put(id, isUnsupportedStatic(id, byId)
            ? computeFallback(id, 0, byId, foods)
            : id);
      }

      log.info("[ItemRegistry] Loaded " + itemsJson.size() + " items, " + foods.size()
          + " foods, created " + remapTable.size() + " remap entries");
      return new ItemRegistry(byId, foods, remapTable);

    } catch (Exception e) {
      log.error("[ItemRegistry] Failed to load item data", e);
      return new ItemRegistry(new Item[0], new HashMap<>(), new HashMap<>());
    }
  }

  @Override
  public boolean isUnsupported(int id) {
    Item item = get(id);
    if (item == null) {
      return id > 0;
    }
    if (UNSUPPORTED_ITEM_NAMES.contains(item.name())) {
      return true;
    }
    return id >= UNSUPPORTED_RANGE_START && id <= UNSUPPORTED_RANGE_END;
  }

  @Override
  public int remap(int id) {
    return remapTable.getOrDefault(id, id);
  }

  public int remap(int id, int meta) {
    return remapTable.getOrDefault(id, id);
  }

  public boolean isFood(int id) {
    return foodsById.containsKey(id);
  }

  public String getDisplayName(int id, int meta) {
    Item item = get(id);
    if (item == null) {
      return "Item #" + id + ":" + meta;
    }
    return item.displayNameFor(meta);
  }

  private record FoodInfo(int id, int foodPoints, float saturation) {

  }
}