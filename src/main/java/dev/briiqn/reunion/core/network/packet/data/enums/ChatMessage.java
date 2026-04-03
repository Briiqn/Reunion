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
public enum ChatMessage {
  CUSTOM(0),
  BED_OCCUPIED(1),
  BED_NO_SLEEP(2),
  BED_NOT_VALID(3),
  BED_NOT_SAFE(4),
  BED_PLAYER_SLEEP(5),
  BED_ME_SLEEP(6),
  PLAYER_LEFT_GAME(7),
  PLAYER_JOINED_GAME(8),
  PLAYER_KICKED_FROM_GAME(9),
  CANNOT_PLACE_LAVA(10),
  DEATH_IN_FIRE(11),
  DEATH_ON_FIRE(12),
  DEATH_LAVA(13),
  DEATH_IN_WALL(14),
  DEATH_DROWN(15),
  DEATH_STARVE(16),
  DEATH_CACTUS(17),
  DEATH_FALL(18),
  DEATH_OUT_OF_WORLD(19),
  DEATH_GENERIC(20),
  DEATH_EXPLOSION(21),
  DEATH_MAGIC(22),
  DEATH_MOB(23),
  DEATH_PLAYER(24),
  DEATH_ARROW(25),
  DEATH_FIREBALL(26),
  DEATH_THROWN(27),
  DEATH_INDIRECT_MAGIC(28),
  DEATH_DRAGON_BREATH(29),
  DEATH_ANVIL(30),
  DEATH_FALLING_BLOCK(31),
  DEATH_THORNS(32),
  DEATH_FELL_ACCIDENT_LADDER(33),
  DEATH_FELL_ACCIDENT_VINES(34),
  DEATH_FELL_ACCIDENT_WATER(35),
  DEATH_FELL_ACCIDENT_GENERIC(36),
  DEATH_FELL_KILLER(37),
  DEATH_FELL_ASSIST(38),
  DEATH_FELL_ASSIST_ITEM(39),
  DEATH_FELL_FINISH(40),
  DEATH_FELL_FINISH_ITEM(41),
  DEATH_IN_FIRE_PLAYER(42),
  DEATH_ON_FIRE_PLAYER(43),
  DEATH_LAVA_PLAYER(44),
  DEATH_DROWN_PLAYER(45),
  DEATH_CACTUS_PLAYER(46),
  DEATH_EXPLOSION_PLAYER(47),
  DEATH_WITHER(48),
  DEATH_PLAYER_ITEM(49),
  DEATH_ARROW_ITEM(50),
  DEATH_FIREBALL_ITEM(51),
  DEATH_THROWN_ITEM(52),
  DEATH_INDIRECT_MAGIC_ITEM(53),
  PLAYER_ENTERED_END(54),
  PLAYER_LEFT_END(55),
  MAX_PIGS_SHEEP_COWS(56),
  MAX_CHICKENS(57),
  MAX_SQUID(58),
  MAX_MOOSHROOMS(59),
  MAX_WOLVES(60),
  MAX_ANIMALS(61),
  MAX_ENEMIES(62),
  MAX_VILLAGERS(63),
  MAX_HANGING_ENTITIES(64),
  CANT_SPAWN_IN_PEACEFUL(65),
  MAX_BRED_ANIMALS(66),
  MAX_BRED_PIGS_SHEEP_COWS(67),
  MAX_BRED_CHICKENS(68),
  MAX_BRED_MOOSHROOMS(69),
  MAX_BRED_WOLVES(70),
  CANT_SHEAR_MOOSHROOM(71),
  MAX_BOATS(72),
  MAX_BATS(73),
  TELEPORT_SUCCESS(74),
  TELEPORT_ME(75),
  TELEPORT_TO_ME(76);

  private final int id;

  ChatMessage(int id) {
    this.id = id;
  }

  public static ChatMessage fromId(int id) {
    for (ChatMessage message : values()) {
      if (message.id == id) {
        return message;
      }
    }
    return null;
  }
}