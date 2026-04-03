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

import java.util.Collection;
import java.util.Optional;

/**
 * A read-only view of the world state as tracked by the proxy for a specific player.
 *
 * <p>Because Reunion is a proxy (not a real server), world state is per-player
 * each player has their own chunk cache, entity list, and position tracking.
 *
 * <p>Obtain via {@code proxy.worldView(player)}.
 */
public interface WorldView {

  /**
   * The dimension the player is currently in.
   */
  Dimension dimension();

  /**
   * Returns the current game time (total tick count, same as Java's GameTime field).
   */
  long gameTime();

  /**
   * Returns the time-of-day value (0–24000, wraps each Minecraft day).
   */
  long dayTime();


  /**
   * Returns the chunk at the given chunk coordinates, or {@code null} if that chunk is not
   * currently in the player's cache.
   *
   * @param chunkX the chunk X (block X >> 4)
   * @param chunkZ the chunk Z (block Z >> 4)
   * @return the cached chunk, or {@code null}
   */
  Chunk getChunk(int chunkX, int chunkZ);

  /**
   * Returns {@code true} if the chunk at the given coordinates is currently loaded in the player's
   * cache.
   */
  boolean isChunkLoaded(int chunkX, int chunkZ);

  /**
   * Returns all currently cached chunks for this player.
   */
  Collection<? extends Chunk> loadedChunks();

  /**
   * Returns the total number of cached chunks.
   */
  int loadedChunkCount();

  /**
   * Looks up the chunk containing the given block coordinates.
   *
   * @param blockX the absolute block X
   * @param blockZ the absolute block Z
   * @return the chunk, or {@code null} if not cached
   */
  default Chunk getChunkAt(int blockX, int blockZ) {
    return getChunk(blockX >> 4, blockZ >> 4);
  }

  /**
   * Returns the block ID at the given world coordinates, using the cached chunk data. Returns
   * {@code 0} (air) if the chunk is not loaded.
   *
   * @param x the absolute block X
   * @param y the block Y (0–255)
   * @param z the absolute block Z
   * @return the block ID
   */
  default int blockAt(int x, int y, int z) {
    Chunk chunk = getChunkAt(x, z);
    if (chunk == null) {
      return 0;
    }
    return chunk.blockIdAt(x & 15, y, z & 15);
  }

  /**
   * Returns the block metadata at the given world coordinates. Returns {@code 0} if the chunk is
   * not loaded.
   */
  default int blockMetaAt(int x, int y, int z) {
    Chunk chunk = getChunkAt(x, z);
    if (chunk == null) {
      return 0;
    }
    return chunk.blockMetaAt(x & 15, y, z & 15);
  }


  /**
   * Returns an entity by its Java-side entity ID, if tracked. Returns an empty Optional if the
   * entity is unknown or has been removed.
   *
   * @param javaEntityId the Java entity ID
   * @return the tracked entity, or empty
   */
  Optional<TrackedEntity> getEntity(int javaEntityId);

  /**
   * Returns all currently tracked entities in this player's view.
   */
  Collection<? extends TrackedEntity> trackedEntities();


  /**
   * Returns the world offset X applied to map Java coordinates to console coordinates. This is used
   * by the infinite world hack to keep console coordinates in range.
   */
  int worldOffsetX();

  /**
   * Returns the world offset Z applied to map Java coordinates to console coordinates.
   */
  int worldOffsetZ();

  /**
   * Converts a Java-side absolute X coordinate to the console-side X coordinate, accounting for the
   * current world offset.
   *
   * @param javaX the Java world X
   * @return the console-side X
   */
  default double toConsoleX(double javaX) {
    return javaX - worldOffsetX();
  }

  /**
   * Converts a Java-side absolute Z coordinate to the console-side Z coordinate.
   *
   * @param javaZ the Java world Z
   * @return the console-side Z
   */
  default double toConsoleZ(double javaZ) {
    return javaZ - worldOffsetZ();
  }
}
