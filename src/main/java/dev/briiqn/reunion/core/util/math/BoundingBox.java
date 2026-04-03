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

package dev.briiqn.reunion.core.util.math;

public record BoundingBox(double minX, double minY, double minZ, double maxX, double maxY,
                          double maxZ) {

  public static final BoundingBox EMPTY = new BoundingBox(0, 0, 0, 0, 0, 0);

  public BoundingBox union(BoundingBox other) {
    if (this == EMPTY) {
      return other;
    }
    if (other == EMPTY) {
      return this;
    }
    return new BoundingBox(
        Math.min(this.minX, other.minX),
        Math.min(this.minY, other.minY),
        Math.min(this.minZ, other.minZ),
        Math.max(this.maxX, other.maxX),
        Math.max(this.maxY, other.maxY),
        Math.max(this.maxZ, other.maxZ)
    );
  }

  public BoundingBox expand(double x, double y, double z) {
    return new BoundingBox(this.minX - x, this.minY - y, this.minZ - z, this.maxX + x,
        this.maxY + y, this.maxZ + z);
  }

  public BoundingBox offset(double x, double y, double z) {
    return new BoundingBox(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x,
        this.maxY + y, this.maxZ + z);
  }

  public boolean intersects(BoundingBox other) {
    return this.maxX > other.minX && this.minX < other.maxX &&
        this.maxY > other.minY && this.minY < other.maxY &&
        this.maxZ > other.minZ && this.minZ < other.maxZ;
  }

  public boolean contains(double x, double y, double z) {
    return x >= this.minX && x <= this.maxX &&
        y >= this.minY && y <= this.maxY &&
        z >= this.minZ && z <= this.maxZ;
  }

  public double getXLength() {
    return this.maxX - this.minX;
  }

  public double getYLength() {
    return this.maxY - this.minY;
  }

  public double getZLength() {
    return this.maxZ - this.minZ;
  }
}