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
 * Represents a 16×256×16 chunk of world data as cached by the proxy.
 *
 * <p>Reunion caches chunks received from the Java backend (optionally to disk).
 * This interface provides access to that cached data.
 *
 * <p>Note: Only chunks that have been received from the Java server and cached
 * are available. Chunks outside the player's view distance return {@code null} from
 * {@link WorldView#getChunk(int, int)}.
 */
public interface Chunk {

  /**
   * The chunk's X coordinate in chunk-space (block X divided by 16).
   */
  int x();

  /**
   * The chunk's Z coordinate in chunk-space (block Z divided by 16).
   */
  int z();

  /**
   * The dimension this chunk belongs to.
   */
  Dimension dimension();

  /**
   * Returns the Java Edition chunk section bitmask. Each bit represents whether a 16×16×16 section
   * (at y = bit × 16) is present in the data.
   */
  int sectionMask();

  /**
   * Returns {@code true} if this is a full-column chunk load (not a partial update).
   */
  boolean isFullChunk();

  /**
   * Returns {@code true} if skylight data is included in this chunk.
   */
  boolean hasSkyLight();

  /**
   * Returns the raw, compressed chunk data bytes as received from the Java server.
   *
   * <p>This is the chunk payload in Java 1.8 chunk data format.
   * Returns an empty array if the data has been evicted from cache.
   */
  byte[] rawData();

  /**
   * Returns the block ID at the given relative coordinates within this chunk.
   *
   * @param localX the block's X offset within this chunk (0–15)
   * @param y      the block's Y coordinate (0–255)
   * @param localZ the block's Z offset within this chunk (0–15)
   * @return the block ID, or {@code 0} (air) if data is unavailable
   */
  int blockIdAt(int localX, int y, int localZ);

  /**
   * Returns the block metadata (damage value) at the given coordinates.
   *
   * @param localX the block's X offset within this chunk (0–15)
   * @param y      the block's Y coordinate (0–255)
   * @param localZ the block's Z offset within this chunk (0–15)
   * @return the metadata nibble (0–15), or {@code 0} if unavailable
   */
  int blockMetaAt(int localX, int y, int localZ);

  /**
   * Returns the world-space X coordinate of the south-west corner of this chunk. Equal to
   * {@link #x()} × 16.
   */
  default int worldX() {
    return x() << 4;
  }

  /**
   * Returns the world-space Z coordinate of the south-west corner of this chunk. Equal to
   * {@link #z()} × 16.
   */
  default int worldZ() {
    return z() << 4;
  }
}
