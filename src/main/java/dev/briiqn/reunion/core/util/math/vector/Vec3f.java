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

public record Vec3f(float x, float y, float z) {

  public static final Vec3f ZERO = new Vec3f(0.0f, 0.0f, 0.0f);

  public Vec3f offset(float dx, float dy, float dz) {
    return new Vec3f(x + dx, y + dy, z + dz);
  }

  public Vec3f add(Vec3f other) {
    return new Vec3f(x + other.x, y + other.y, z + other.z);
  }

  public Vec3f sub(Vec3f other) {
    return new Vec3f(x - other.x, y - other.y, z - other.z);
  }

  public Vec3f mul(float scalar) {
    return new Vec3f(x * scalar, y * scalar, z * scalar);
  }

  public float dot(Vec3f other) {
    return x * other.x + y * other.y + z * other.z;
  }

  public Vec3f cross(Vec3f other) {
    return new Vec3f(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    );
  }

  public float lengthSq() {
    return x * x + y * y + z * z;
  }

  public float length() {
    return MathUtil.sqrt(lengthSq());
  }

  public Vec3f normalize() {
    float len = length();
    return len < 1.0E-4f ? ZERO : new Vec3f(x / len, y / len, z / len);
  }

  public Vec3d toDouble() {
    return new Vec3d(x, y, z);
  }
}