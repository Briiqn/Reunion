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
public enum ClientCommand {
  LOGIN_COMPLETE(0),
  PERFORM_RESPAWN(1);

  private final int id;

  ClientCommand(int id) {
    this.id = id;
  }

  public static ClientCommand fromId(int id) {
    for (ClientCommand command : values()) {
      if (command.id == id) {
        return command;
      }
    }
    return null;
  }
}