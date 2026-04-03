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

package dev.briiqn.reunion.core.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;

public final class Block {

  private final int id;
  private final String name;
  private final String displayName;
  private final String boundingBox;
  private final float hardness;
  private final String material;
  private final Int2ObjectMap<String> variations;
  private final int[] collisionShapes;

  public Block(
      int id,
      String name,
      String displayName,
      String boundingBox,
      float hardness,
      String material,
      Map<Integer, String> variations,
      int[] collisionShapes
  ) {
    this.id = id;
    this.name = name;
    this.displayName = displayName;
    this.boundingBox = boundingBox;
    this.hardness = hardness;
    this.material = material == null ? "" : material;
    this.collisionShapes = collisionShapes;
    if (variations == null || variations.isEmpty()) {
      this.variations = Int2ObjectMaps.emptyMap();
    } else {
      Int2ObjectOpenHashMap<String> map = new Int2ObjectOpenHashMap<>(variations);
      this.variations = Int2ObjectMaps.unmodifiable(map);
    }
  }

  public int id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String displayName() {
    return displayName;
  }

  public String boundingBox() {
    return boundingBox;
  }

  public String material() {
    return material;
  }

  public float hardness() {
    return hardness;
  }

  public Int2ObjectMap<String> variations() {
    return variations;
  }

  public int resolveMetadata(int meta) {
    if (variations.isEmpty()) {
      return meta;
    }
    if (variations.containsKey(meta)) {
      return meta;
    }

    int bestKey = -1;
    int bestDist = Integer.MAX_VALUE;
    for (int key : variations.keySet()) {
      int dist = Math.abs(key - meta);
      if (dist < bestDist || (dist == bestDist && key > bestKey)) {
        bestDist = dist;
        bestKey = key;
      }
    }
    return bestKey == -1 ? 0 : bestKey;
  }

  public int collisionShapeFor(int meta) {
    if (collisionShapes.length == 1) {
      return collisionShapes[0];
    }
    int resolved = resolveMetadata(meta);
    int clamped = Math.max(0, Math.min(resolved, collisionShapes.length - 1));
    return collisionShapes[clamped];
  }

  public String displayNameFor(int meta) {
    if (variations.isEmpty()) {
      return displayName;
    }
    return variations.getOrDefault(resolveMetadata(meta), displayName);
  }

  public boolean isAlwaysEmpty() {
    for (int s : collisionShapes) {
      if (s != 0) {
        return false;
      }
    }
    return true;
  }

  public boolean isFullCube(int meta) {
    return collisionShapeFor(meta) == 1;
  }

  @Override
  public String toString() {
    return "Block{id=" + id + ", name='" + name + "', displayName='" + displayName + "'}";
  }
}