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
public enum GamePrivilege {
  BUILD_AND_MINE(1 << 0),
  USE_DOORS_AND_SWITCHES(1 << 1),
  OPEN_CONTAINERS(1 << 2),
  ATTACK_PLAYERS(1 << 3),
  ATTACK_MOBS(1 << 4),
  TRUST_PLAYERS(1 << 5);

  private final int bit;

  GamePrivilege(int bit) {
    this.bit = bit;
  }


  public static int grant(GamePrivilege... grantedPrivileges) {
    int mask = 0b111111;

    for (GamePrivilege privilege : grantedPrivileges) {
      mask &= ~privilege.getBit();
    }

    return mask;
  }


  public static int grantAll() {
    return 0;
  }
}