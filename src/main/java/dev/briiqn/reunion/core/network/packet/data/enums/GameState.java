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
public enum GameState {
  INVALID_BED(0),
  END_RAINING(1),
  BEGIN_RAINING(2),
  CHANGE_GAME_MODE(3),
  ENTER_CREDITS(4),
  DEMO_MESSAGE(5),
  ARROW_HIT(6),
  FADE_VALUE(7),
  FADE_TIME(8),
  PLAY_PUFFERFISH_AMBIENT_SOUND(9),
  PLAY_ELDER_GUARDIAN_MOB_APPEARANCE(10);

  private final int id;

  GameState(int id) {
    this.id = id;
  }

  public static GameState fromId(int id) {
    for (GameState state : values()) {
      if (state.id == id) {
        return state;
      }
    }
    return null;
  }
}