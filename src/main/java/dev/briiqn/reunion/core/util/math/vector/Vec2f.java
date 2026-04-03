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

package dev.briiqn.reunion.core.util.math.vector;

import dev.briiqn.reunion.core.util.math.MathUtil;

public record Vec2f(float yaw, float pitch) {

  public static final Vec2f ZERO = new Vec2f(0f, 0f);

  public boolean hasChanged(Vec2f other) {
    return Float.compare(this.yaw, other.yaw) != 0
        || Float.compare(this.pitch, other.pitch) != 0;
  }

  public Vec2f withYaw(float newYaw) {
    return new Vec2f(newYaw, pitch);
  }

  public Vec2f withPitch(float newPitch) {
    return new Vec2f(yaw, newPitch);
  }

  public Vec2f add(float dYaw, float dPitch) {
    return new Vec2f(yaw + dYaw, pitch + dPitch);
  }

  public Vec2f wrap() {
    return new Vec2f(MathUtil.wrapAngleTo180(yaw), MathUtil.wrapAngleTo180(pitch));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Vec2f(float yaw1, float pitch1))) {
      return false;
    }
    return Float.compare(yaw, yaw1) == 0 && Float.compare(pitch, pitch1) == 0;
  }

  @Override
  public int hashCode() {
    int result = Float.hashCode(yaw);
    result = 31 * result + Float.hashCode(pitch);
    return result;
  }

  @Override
  public String toString() {
    return "Vec2f{yaw=" + yaw + ", pitch=" + pitch + "}";
  }
}