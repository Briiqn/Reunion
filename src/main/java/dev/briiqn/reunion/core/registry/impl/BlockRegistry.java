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
import dev.briiqn.reunion.core.data.Block;
import dev.briiqn.reunion.core.registry.ArrayRegistry;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class BlockRegistry extends ArrayRegistry<Block> {

  private static final Set<String> UNSUPPORTED_BLOCKS = Set.of(
      "stained_glass",/* "beacon",*/ "wooden_button",
      "light_weighted_pressure_plate", "heavy_weighted_pressure_plate",
      "unpowered_comparator", "powered_comparator", "daylight_detector",
      "redstone_block", "quartz_ore", "hopper", "quartz_block",
      "quartz_stairs", "activator_rail", "dropper", "stained_hardened_clay",
      "stained_glass_pane", "leaves2", "log2", "acacia_stairs",
      "dark_oak_stairs", "slime", "barrier", "iron_trapdoor",
      "prismarine", "sea_lantern", "hay_block", /*"carpet",*/
      "hardened_clay", "coal_block", "packed_ice", "double_plant",
      "standing_banner", "wall_banner", "daylight_detector_inverted",
      "red_sandstone", "red_sandstone_stairs", "double_stone_slab2",
      "stone_slab2", "spruce_fence_gate", "birch_fence_gate",
      "jungle_fence_gate", "dark_oak_fence_gate", "acacia_fence_gate",
      "spruce_fence", "birch_fence", "jungle_fence", "dark_oak_fence",
      "acacia_fence", "spruce_door", "birch_door", "jungle_door",
      "acacia_door", "dark_oak_door"
  );
  private static final Set<String> FLUID_BLOCKS = Set.of(
      "water", "flowing_water", "lava", "flowing_lava"
  );
  private static final double W_NAME = 0.40;
  private static final double W_SHAPE = 0.35;
  private static final double W_HARDNESS = 0.15;
  private static final double W_MATERIAL = 0.10;
  private static final float HARDNESS_MAX = 50f;
  private static volatile BlockRegistry INSTANCE;
  private final Int2IntMap remapTable;

  private BlockRegistry(Block[] entries, Int2IntMap remapTable) {
    super(entries);
    this.remapTable = Int2IntMaps.unmodifiable(remapTable);
  }

  public static BlockRegistry getInstance() {
    if (INSTANCE == null) {
      synchronized (BlockRegistry.class) {
        if (INSTANCE == null) {
          INSTANCE = load();
        }
      }
    }
    return INSTANCE;
  }

  private static int computeFallbackId(int id, Block[] entries) {
    Block original = entries.length > id ? entries[id] : null;
    if (original == null) {
      return 1;
    }

    String targetName = original.displayNameFor(0).toLowerCase(Locale.ROOT);
    Set<String> targetWords = wordSet(targetName);
    int targetShape = original.collisionShapeFor(0);
    String targetBB = original.boundingBox();
    float targetHardness = original.hardness();
    String targetMaterial = original.material();

    Block best = null;
    double bestScore = Double.MAX_VALUE;

    for (Block candidate : entries) {
      if (candidate == null) {
        continue;
      }
      Block cb = entries.length > candidate.id() ? entries[candidate.id()] : null;
      if (cb == null || UNSUPPORTED_BLOCKS.contains(cb.name())) {
        continue;
      }
      double score = score(candidate, targetShape, targetBB, targetName, targetWords,
          targetHardness, targetMaterial);
      if (score < bestScore) {
        bestScore = score;
        best = candidate;
      }
    }

    return best != null ? best.id() : 1;
  }

  private static double score(Block candidate,
      int targetShape, String targetBB,
      String targetName, Set<String> targetWords,
      float targetHardness, String targetMaterial) {
    String candidateName = candidate.displayName().toLowerCase(Locale.ROOT);

    double nameScore;
    if (targetWords.isEmpty()) {
      int maxLen = Math.max(targetName.length(), candidateName.length());
      nameScore = maxLen == 0 ? 0.0 : (double) editDistance(targetName, candidateName) / maxLen;
    } else {
      Set<String> candidateWords = wordSet(candidateName);
      long intersection = targetWords.stream().filter(candidateWords::contains).count();
      long union = targetWords.size() + candidateWords.size() - intersection;
      nameScore = 1.0 - (union == 0 ? 1.0 : (double) intersection / union);
    }

    double shapeScore = candidate.collisionShapeFor(0) == targetShape ? 0.0
        : targetBB.equals(candidate.boundingBox()) ? 0.5 : 1.0;
    double hardnessScore = Math.min(Math.abs(candidate.hardness() - targetHardness) / HARDNESS_MAX,
        1.0);
    double materialScore = targetMaterial.equals(candidate.material()) ? 0.0 : 1.0;

    return W_NAME * nameScore + W_SHAPE * shapeScore
        + W_HARDNESS * hardnessScore + W_MATERIAL * materialScore;
  }

  private static Set<String> wordSet(String name) {
    Set<String> words = new HashSet<>();
    for (String token : name.split("[^a-z]+")) {
      if (!token.isEmpty()) {
        words.add(token);
      }
    }
    return words;
  }

  public static int editDistance(String a, String b) {
    int m = a.length(), n = b.length();
    int[] dp = new int[n + 1];
    for (int j = 0; j <= n; j++) {
      dp[j] = j;
    }
    for (int i = 1; i <= m; i++) {
      int prev = dp[0];
      dp[0] = i;
      for (int j = 1; j <= n; j++) {
        int temp = dp[j];
        dp[j] = a.charAt(i - 1) == b.charAt(j - 1)
            ? prev : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
        prev = temp;
      }
    }
    return dp[n];
  }

  private static BlockRegistry load() {
    try {
      JSONArray blocksJson = readJsonArray(BlockRegistry.class, "/blocks.json");
      JSONObject shapesRoot = readJsonObject(BlockRegistry.class, "/blockCollisionShapes.json");

      JSONObject shapesMap = shapesRoot.getJSONObject("blocks");
      Map<String, int[]> collisionShapesByName = new HashMap<>();
      for (String blockName : shapesMap.keySet()) {
        Object val = shapesMap.get(blockName);
        if (val instanceof Number n) {
          collisionShapesByName.put(blockName, new int[]{n.intValue()});
        } else if (val instanceof JSONArray arr) {
          int[] shapes = new int[arr.size()];
          for (int i = 0; i < arr.size(); i++) {
            shapes[i] = arr.getInteger(i);
          }
          collisionShapesByName.put(blockName, shapes);
        }
      }

      int maxId = 0;
      for (int i = 0; i < blocksJson.size(); i++) {
        int id = blocksJson.getJSONObject(i).getIntValue("id");
        if (id > maxId) {
          maxId = id;
        }
      }

      Block[] byId = new Block[maxId + 1];
      for (int i = 0; i < blocksJson.size(); i++) {
        JSONObject obj = blocksJson.getJSONObject(i);
        int id = obj.getIntValue("id");
        String name = obj.getString("name");
        String displayName = obj.getString("displayName");
        String boundingBox = obj.getString("boundingBox");
        float hardness = obj.getFloatValue("hardness");
        String material = obj.containsKey("material") ? obj.getString("material") : "";

        Map<Integer, String> variations = new LinkedHashMap<>();
        JSONArray varArray = obj.getJSONArray("variations");
        if (varArray != null) {
          for (int v = 0; v < varArray.size(); v++) {
            JSONObject var = varArray.getJSONObject(v);
            variations.put(var.getIntValue("metadata"), var.getString("displayName"));
          }
        }

        int[] shapes = collisionShapesByName.getOrDefault(name,
            "block".equals(boundingBox) ? new int[]{1} : new int[]{0});

        byId[id] = new Block(id, name, displayName, boundingBox, hardness, material, variations,
            shapes);
      }

      Int2IntOpenHashMap remapTable = new Int2IntOpenHashMap(byId.length);
      for (int id = 0; id < byId.length; id++) {
        if (byId[id] == null) {
          continue;
        }
        remapTable.put(id, UNSUPPORTED_BLOCKS.contains(byId[id].name())
            ? computeFallbackId(id, byId)
            : id);
      }

      log.info(
          "[BlockRegistry] Loaded " + blocksJson.size() + " blocks (max id=" + maxId + "), created "
              + remapTable.size() + " remap entries");
      return new BlockRegistry(byId, remapTable);

    } catch (Exception e) {
      log.error("[BlockRegistry] Failed to load block data", e);
      return new BlockRegistry(new Block[0], new Int2IntOpenHashMap());
    }
  }

  @Override
  public boolean isUnsupported(int id) {
    Block b = get(id);
    return b == null || UNSUPPORTED_BLOCKS.contains(b.name());
  }

  @Override
  public int remap(int id) {
    return remapTable.getOrDefault(id, id);
  }

  public boolean isFluid(int id) {
    Block b = get(id);
    return b != null && FLUID_BLOCKS.contains(b.name());
  }

  public String getDisplayName(int id, int meta) {
    Block b = get(id);
    if (b == null) {
      return "Block #" + id + ":" + meta;
    }
    return b.displayNameFor(meta);
  }

  public int[] remapBlock(int id, int meta) {
    int remappedId = remapTable.getOrDefault(id, id);

    if (id == 50 || id == 76) { //torch, redstone torch
      // Java and LCE both use 5 for floor
      return new int[]{remappedId, meta};
    }

//LCE does not support floor buttons
    Block b = get(remappedId);

    if (b == null || UNSUPPORTED_BLOCKS.contains(b.name())) {
      remappedId = 1;
      b = get(1);
    }

    return new int[]{remappedId, b != null ? b.resolveMetadata(meta) : 0};
  }
}