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

package dev.briiqn.reunion.api.world;

/**
 * An immutable integer block coordinate triple.
 *
 * <p>Convenience methods allow easy navigation to adjacent blocks and
 * conversion to chunk coordinates.
 */
public record BlockPosition(int x, int y, int z) {

  /**
   * A zero-origin BlockPosition.
   */
  public static final BlockPosition ZERO = new BlockPosition(0, 0, 0);

  /**
   * Decodes a Java 1.8 packed long into a BlockPosition.
   */
  public static BlockPosition fromPackedLong(long packed) {
    int x = (int) (packed >> 38);
    int y = (int) ((packed >> 26) & 0xFFF);
    int z = (int) (packed << 38 >> 38);
    return new BlockPosition(x, y, z);
  }

  /**
   * Returns the chunk X this block falls in.
   */
  public int chunkX() {
    return x >> 4;
  }

  /**
   * Returns the chunk Z this block falls in.
   */
  public int chunkZ() {
    return z >> 4;
  }

  /**
   * Returns the block's X offset within its chunk (0–15).
   */
  public int localX() {
    return x & 15;
  }

  /**
   * Returns the block's Z offset within its chunk (0–15).
   */
  public int localZ() {
    return z & 15;
  }

  /**
   * Returns the block at (x+dx, y+dy, z+dz).
   */
  public BlockPosition offset(int dx, int dy, int dz) {
    return new BlockPosition(x + dx, y + dy, z + dz);
  }

  /**
   * Returns the block one step up (y+1).
   */
  public BlockPosition up() {
    return new BlockPosition(x, y + 1, z);
  }

  /**
   * Returns the block one step down (y-1).
   */
  public BlockPosition down() {
    return new BlockPosition(x, y - 1, z);
  }

  /**
   * Returns the block to the north (z-1).
   */
  public BlockPosition north() {
    return new BlockPosition(x, y, z - 1);
  }

  /**
   * Returns the block to the south (z+1).
   */
  public BlockPosition south() {
    return new BlockPosition(x, y, z + 1);
  }

  /**
   * Returns the block to the east (x+1).
   */
  public BlockPosition east() {
    return new BlockPosition(x + 1, y, z);
  }

  /**
   * Returns the block to the west (x-1).
   */
  public BlockPosition west() {
    return new BlockPosition(x - 1, y, z);
  }

  /**
   * Returns a {@link Location} at the centre of this block (x+0.5, y+0.5, z+0.5) in the given
   * dimension with default orientation.
   */
  public Location toLocation(Dimension dimension) {
    return Location.of(x + 0.5, y + 0.5, z + 0.5, dimension);
  }

  /**
   * Encodes this position into the Java 1.8 packed long format:
   * {@code ((x & 0x3FFFFFF) << 38) | ((y & 0xFFF) << 26) | (z & 0x3FFFFFF)}.
   */
  public long toPackedLong() {
    return (((long) (x & 0x3FFFFFF)) << 38) | (((long) (y & 0xFFF)) << 26) | ((long) (z
        & 0x3FFFFFF));
  }

  @Override
  public String toString() {
    return "BlockPosition{" + x + ", " + y + ", " + z + "}";
  }
}
