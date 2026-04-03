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
public enum EntityType {
  ZOMBIE(54),
  PIG_ZOMBIE(57),
  ARROW(60),
  SNOWBALL(61),
  EGG(62),
  FIREBALL(63),
  SMALL_FIREBALL(64),
  ENDER_PEARL(65),
  WITHER_SKULL(66),
  FALLING_BLOCK(70),
  ITEM_FRAME(71),
  EYE_OF_ENDER(72),
  POTION(73),
  EXP_BOTTLE(75),
  ARMOR_STAND(78),
  FISHING_HOOK(90);

  private final int id;

  EntityType(int id) {
    this.id = id;
  }
}