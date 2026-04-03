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
public enum TextureChangeType {
  SKIN(0),
  CAPE(1);

  private final int id;

  TextureChangeType(int id) {
    this.id = id;
  }

  public static TextureChangeType fromId(int id) {
    for (TextureChangeType type : values()) {
      if (type.id == id) {
        return type;
      }
    }
    return null;
  }
}