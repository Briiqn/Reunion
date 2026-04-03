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
public enum ConsoleGameEvent {
  NO_RESPAWN_BED_AVAILABLE(0),
  START_RAINING(1),
  STOP_RAINING(2),
  CHANGE_GAME_MODE(3),
  WIN_GAME(4),
  DEMO_EVENT(5),
  SUCCESSFUL_BOW_HIT(6),
  START_SAVING(10),
  STOP_SAVING(11);

  private final int id;

  ConsoleGameEvent(int id) {
    this.id = id;
  }

  public static ConsoleGameEvent fromId(int id) {
    for (ConsoleGameEvent event : values()) {
      if (event.id == id) {
        return event;
      }
    }
    return null;
  }
}