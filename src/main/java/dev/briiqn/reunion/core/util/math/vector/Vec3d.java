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

public record Vec3d(double x, double y, double z) {

  public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);

  public Vec3d offset(double dx, double dy, double dz) {
    return new Vec3d(x + dx, y + dy, z + dz);
  }

  public Vec3d add(Vec3d other) {
    return new Vec3d(x + other.x, y + other.y, z + other.z);
  }

  public Vec3d sub(Vec3d other) {
    return new Vec3d(x - other.x, y - other.y, z - other.z);
  }

  public Vec3d mul(double scalar) {
    return new Vec3d(x * scalar, y * scalar, z * scalar);
  }

  public double dot(Vec3d other) {
    return x * other.x + y * other.y + z * other.z;
  }

  public Vec3d cross(Vec3d other) {
    return new Vec3d(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    );
  }

  public Vec3d withX(double nx) {
    return new Vec3d(nx, y, z);
  }

  public Vec3d withY(double ny) {
    return new Vec3d(x, ny, z);
  }

  public Vec3d withZ(double nz) {
    return new Vec3d(x, y, nz);
  }

  public double distSq(Vec3d other) {
    double dx = this.x - other.x;
    double dy = this.y - other.y;
    double dz = this.z - other.z;
    return dx * dx + dy * dy + dz * dz;
  }

  public double dist(Vec3d other) {
    return MathUtil.sqrt(distSq(other));
  }

  public double lengthSq() {
    return x * x + y * y + z * z;
  }

  public double length() {
    return MathUtil.sqrt(lengthSq());
  }

  public Vec3d normalize() {
    double len = length();
    return len < 1.0E-4 ? ZERO : new Vec3d(x / len, y / len, z / len);
  }

  public Vec3f toFloat() {
    return new Vec3f((float) x, (float) y, (float) z);
  }

  public Vec3i toBlock() {
    return new Vec3i(MathUtil.floor(x), MathUtil.floor(y), MathUtil.floor(z));
  }

  public int blockX() {
    return MathUtil.floor(x);
  }

  public int blockY() {
    return MathUtil.floor(y);
  }

  public int blockZ() {
    return MathUtil.floor(z);
  }

  public int chunkX() {
    return blockX() >> 4;
  }

  public int chunkZ() {
    return blockZ() >> 4;
  }
}