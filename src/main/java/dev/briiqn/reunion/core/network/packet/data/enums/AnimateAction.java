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
public enum AnimateAction {
  SWING(1),
  HURT(2),
  WAKE_UP(3),
  RESPAWN(4),
  EAT(5),
  CRITICAL_HIT(6),
  MAGIC_CRITICAL_HIT(7);

  private final int id;

  AnimateAction(int id) {
    this.id = id;
  }

  public static AnimateAction fromId(int id) {
    for (AnimateAction action : values()) {
      if (action.id == id) {
        return action;
      }
    }
    return null;
  }
}