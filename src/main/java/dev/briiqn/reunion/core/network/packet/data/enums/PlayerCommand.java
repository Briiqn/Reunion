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
public enum PlayerCommand {
  START_SNEAKING(1, 0),
  STOP_SNEAKING(2, 1),
  STOP_SLEEPING(3, 2),
  START_SPRINTING(4, 3),
  STOP_SPRINTING(5, 4),
  RIDING_JUMP(8, 5),
  OPEN_INVENTORY(9, 6);

  private final int consoleId;
  private final int javaId;

  PlayerCommand(int consoleId, int javaId) {
    this.consoleId = consoleId;
    this.javaId = javaId;
  }

  public static PlayerCommand fromConsoleId(int consoleId) {
    for (PlayerCommand action : values()) {
      if (action.consoleId == consoleId) {
        return action;
      }
    }
    return null;
  }
}