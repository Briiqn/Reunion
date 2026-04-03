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
public enum ServerSetting {
  DIFFICULTY_PEACEFUL(0x00),
  DIFFICULTY_EASY(0x01),
  DIFFICULTY_NORMAL(0x02),
  DIFFICULTY_HARD(0x03),
  FRIENDS_OF_FRIENDS(0x04),
  SHOW_GAMERTAGS(0x08),
  CREATIVE_MODE(0x10),
  FLAT_WORLD(0x40),
  GENERATE_STRUCTURES(0x80),
  BONUS_CHEST(0x100),
  PVP_ENABLED(0x400),
  TRUST_PLAYERS(0x800),
  TNT_ENABLED(0x1000),
  SPREAD_FIRE(0x2000);

  private final int bit;

  ServerSetting(int bit) {
    this.bit = bit;
  }

  public static int build(ServerSetting... settings) {
    int mask = 0;
    for (ServerSetting setting : settings) {
      mask |= setting.getBit();
    }
    return mask;
  }
}