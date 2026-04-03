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

public record Item(int id, String name, String displayName, int stackSize,
                   Int2ObjectMap<String> variations) {

  public Item(
      int id,
      String name,
      String displayName,
      int stackSize,
      Map<Integer, String> variations
  ) {
    this(id, name, displayName, stackSize, variations == null || variations.isEmpty()
        ? Int2ObjectMaps.emptyMap()
        : Int2ObjectMaps.unmodifiable(new Int2ObjectOpenHashMap<>(variations)));
  }

  public String displayNameFor(int meta) {
    return variations.getOrDefault(meta, displayName);
  }

  @Override
  public String toString() {
    return "Item{id=" + id + ", name='" + name + "', displayName='" + displayName + "'}";
  }
}