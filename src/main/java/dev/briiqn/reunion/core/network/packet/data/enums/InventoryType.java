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

package dev.briiqn.reunion.core.network.packet.data.enums;

import lombok.Getter;

@Getter
public enum InventoryType {
  CONTAINER(0, "minecraft:container"),
  CRAFTING_TABLE(1, "minecraft:crafting_table"),
  FURNACE(2, "minecraft:furnace"),
  DISPENSER(3, "minecraft:dispenser"),
  ENCHANTING_TABLE(4, "minecraft:enchanting_table"),
  BREWING_STAND(5, "minecraft:brewing_stand"),
  VILLAGER(6, "minecraft:villager"),
  BEACON(7, "minecraft:beacon"),
  ANVIL(8, "minecraft:anvil"),
  HOPPER(9, "minecraft:hopper"),
  DROPPER(10, "minecraft:dropper"),
  HORSE(11, "EntityHorse");

  private final int id;
  private final String identifier;

  InventoryType(int id, String identifier) {
    this.id = id;
    this.identifier = identifier;
  }

  public static InventoryType from(String identifier) {
    for (InventoryType type : values()) {
      if (type.identifier.equals(identifier)) {
        return type;
      }
    }
    return CONTAINER;
  }
}