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

public class Vec3i {

  public static final Vec3i ZERO = new Vec3i(0, 0, 0);
  private static final int BITS_X = 26;
  private static final int BITS_Y = 12;
  private static final int BITS_Z = 26;
  private static final long MASK_X = (1L << BITS_X) - 1L;
  private static final long MASK_Y = (1L << BITS_Y) - 1L;
  private static final long MASK_Z = (1L << BITS_Z) - 1L;
  private static final int SHIFT_X = BITS_Y + BITS_Z;
  private static final int SHIFT_Y = BITS_Z;
  protected final long packed;

  public Vec3i(int x, int y, int z) {
    this.packed = (((long) x & MASK_X) << SHIFT_X)
        | (((long) y & MASK_Y) << SHIFT_Y)
        | ((long) z & MASK_Z);
  }

  protected Vec3i(long packed) {
    this.packed = packed;
  }

  public static Vec3i fromPackedLong(long wire) {
    return new Vec3i(wire);
  }

  public int x() {
    return (int) (packed >> SHIFT_X);
  }

  public int y() {
    int raw = (int) ((packed >> SHIFT_Y) & MASK_Y);
    return (raw << (32 - BITS_Y)) >> (32 - BITS_Y);
  }

  public int z() {
    return (int) (packed << (64 - BITS_Z) >> (64 - BITS_Z));
  }

  public Vec3i offset(int dx, int dy, int dz) {
    return new Vec3i(x() + dx, y() + dy, z() + dz);
  }

  public Vec3i add(Vec3i other) {
    return offset(other.x(), other.y(), other.z());
  }

  public Vec3i sub(Vec3i other) {
    return offset(-other.x(), -other.y(), -other.z());
  }

  public Vec3i mul(int scalar) {
    return new Vec3i(x() * scalar, y() * scalar, z() * scalar);
  }

  public Vec3i up() {
    return offset(0, 1, 0);
  }

  public Vec3i down() {
    return offset(0, -1, 0);
  }

  public Vec3i north() {
    return offset(0, 0, -1);
  }

  public Vec3i south() {
    return offset(0, 0, 1);
  }

  public Vec3i east() {
    return offset(1, 0, 0);
  }

  public Vec3i west() {
    return offset(-1, 0, 0);
  }

  public int chunkX() {
    return x() >> 4;
  }

  public int chunkZ() {
    return z() >> 4;
  }

  public long distSq(Vec3i other) {
    long dx = (long) x() - other.x();
    long dy = (long) y() - other.y();
    long dz = (long) z() - other.z();
    return dx * dx + dy * dy + dz * dz;
  }

  public double dist(Vec3i other) {
    return MathUtil.sqrt(distSq(other));
  }

  public long toPackedLong() {
    return packed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Vec3i v)) {
      return false;
    }
    return packed == v.packed;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(packed);
  }

  @Override
  public String toString() {
    return "Vec3i{" + x() + ", " + y() + ", " + z() + "}";
  }
}