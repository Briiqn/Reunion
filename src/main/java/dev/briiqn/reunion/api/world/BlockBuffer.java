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

import dev.briiqn.reunion.core.world.SimpleBlockBuffer;

/**
 * A buffer for setting blocks in a 3D region.
 * <p>
 * Create instances via {@link #create(int, int, int)}. The API implementation will handle the
 * low-level encoding when this is sent to a player.
 */
public interface BlockBuffer {

  /**
   * Creates a new block buffer with the given dimensions. Dimensions are limited to 16x256x16.
   */
  static BlockBuffer create(int width, int height, int depth) {
    return new SimpleBlockBuffer(width, height, depth);
  }

  /**
   * Sets a block at the given relative coordinates within the buffer.
   */
  void setBlock(int x, int y, int z, int id, int meta);
}