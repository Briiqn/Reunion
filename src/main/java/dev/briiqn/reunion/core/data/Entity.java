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

package dev.briiqn.reunion.core.data;

public record Entity(int id, int internalId, String name, String displayName, Type type,
                     float width, float height,
                     String category) {

  public boolean isMob() {
    return type == Type.MOB;
  }

  public boolean isObject() {
    return type == Type.OBJECT;
  }

  public boolean hasBoundingBox() {
    return !(width >= 0) || !(height >= 0);
  }

  @Override
  public String toString() {
    return "Entity{id=" + id + ", name='" + name + "', type=" + type + "}";
  }

  public enum Type {MOB, OBJECT}
}