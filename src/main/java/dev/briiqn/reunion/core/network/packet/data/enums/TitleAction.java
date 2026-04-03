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
public enum TitleAction {
  TITLE(0),
  SUBTITLE(1),
  TIMES(2),
  CLEAR(3),
  RESET(4);

  private final int id;

  TitleAction(int id) {
    this.id = id;
  }

  public static TitleAction fromId(int id) {
    for (TitleAction action : values()) {
      if (action.id == id) {
        return action;
      }
    }
    return null;
  }
}