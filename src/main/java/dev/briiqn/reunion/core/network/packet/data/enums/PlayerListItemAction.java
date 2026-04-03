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
public enum PlayerListItemAction {
  ADD_PLAYER(0),
  UPDATE_GAMEMODE(1),
  UPDATE_LATENCY(2),
  UPDATE_DISPLAY_NAME(3),
  REMOVE_PLAYER(4);

  private final int id;

  PlayerListItemAction(int id) {
    this.id = id;
  }

  public static PlayerListItemAction fromId(int id) {
    for (PlayerListItemAction action : values()) {
      if (action.id == id) {
        return action;
      }
    }
    return null;
  }
}