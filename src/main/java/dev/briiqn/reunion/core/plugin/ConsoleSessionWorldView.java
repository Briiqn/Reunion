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

package dev.briiqn.reunion.core.plugin;

import dev.briiqn.reunion.api.world.Chunk;
import dev.briiqn.reunion.api.world.Dimension;
import dev.briiqn.reunion.api.world.TrackedEntity;
import dev.briiqn.reunion.api.world.WorldView;
import dev.briiqn.reunion.core.manager.ChunkManager;
import dev.briiqn.reunion.core.manager.EntityManager;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implements the plugin-facing {@link WorldView} by delegating to the session's
 * {@link ChunkManager} and {@link EntityManager}.
 */
final class ConsoleSessionWorldView implements WorldView {

  private final ConsoleSession session;

  ConsoleSessionWorldView(ConsoleSession session) {
    this.session = session;
  }

  @Override
  public Dimension dimension() {
    return Dimension.fromIdOrDefault(session.getDimension());
  }

  @Override
  public long gameTime() {
    return session.getGameTime();
  }

  @Override
  public long dayTime() {
    return session.getDayTime();
  }


  @Override
  public Chunk getChunk(int chunkX, int chunkZ) {
    ChunkManager cm = session.getChunkManager();
    if (cm == null) {
      return null;
    }
    ChunkManager.CachedChunk raw = cm.getCachedChunk(chunkX, chunkZ);
    return raw == null ? null : new CachedChunkAdapter(raw);
  }

  @Override
  public boolean isChunkLoaded(int chunkX, int chunkZ) {
    return getChunk(chunkX, chunkZ) != null;
  }

  @Override
  public Collection<? extends Chunk> loadedChunks() {
    ChunkManager cm = session.getChunkManager();
    if (cm == null) {
      return Collections.emptyList();
    }
    List<Chunk> result = new ArrayList<>();
    for (ChunkManager.CachedChunk raw : cm.getAllCachedChunks()) {
      result.add(new CachedChunkAdapter(raw));
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public int loadedChunkCount() {
    ChunkManager cm = session.getChunkManager();
    return cm == null ? 0 : cm.getCacheSize();
  }

  @Override
  public Optional<TrackedEntity> getEntity(int javaEntityId) {
    EntityManager em = session.getEntityManager();
    Integer consoleId = em.getConsoleId(javaEntityId);
    if (consoleId == null) {
      return Optional.empty();
    }
    return Optional.of(new EntityManagerTrackedEntity(em, javaEntityId, consoleId));
  }

  @Override
  public Collection<? extends TrackedEntity> trackedEntities() {
    EntityManager em = session.getEntityManager();
    List<TrackedEntity> entities = new ArrayList<>();
    for (int javaId : em.getJavaIds()) {
      Integer consoleId = em.getConsoleId(javaId);
      if (consoleId != null) {
        entities.add(new EntityManagerTrackedEntity(em, javaId, consoleId));
      }
    }
    return Collections.unmodifiableList(entities);
  }

  @Override
  public int worldOffsetX() {
    return session.getWorldOffsetX();
  }

  @Override
  public int worldOffsetZ() {
    return session.getWorldOffsetZ();
  }

  private record CachedChunkAdapter(ChunkManager.CachedChunk raw) implements Chunk {

    @Override
    public int x() {
      return raw.x();
    }

    @Override
    public int z() {
      return raw.z();
    }

    @Override
    public Dimension dimension() {
      return Dimension.fromIdOrDefault(raw.dimension());
    }

    @Override
    public int sectionMask() {
      return raw.mask();
    }

    @Override
    public boolean isFullChunk() {
      return raw.fullChunk();
    }

    @Override
    public boolean hasSkyLight() {
      return raw.hasSky();
    }

    @Override
    public byte[] rawData() {
      return raw.getData();
    }

    @Override
    public int blockIdAt(int localX, int y, int localZ) {
      byte[] data = raw.getData();
      if (data == null || data.length == 0) {
        return 0;
      }
      int section = y >> 4;
      int localY = y & 15;
      if ((raw.mask() & (1 << section)) == 0) {
        return 0;
      }

      int sectionsBefore = Integer.bitCount(raw.mask() & ((1 << section) - 1));
      int sectionStart = sectionsBefore * 8192;
      int blockIndex = (localY << 8) | (localZ << 4) | localX;
      int byteOffset = sectionStart + blockIndex * 2;
      if (byteOffset + 1 >= data.length) {
        return 0;
      }

      int bam = (data[byteOffset] & 0xFF) | ((data[byteOffset + 1] & 0xFF) << 8);
      return bam >> 4;
    }

    @Override
    public int blockMetaAt(int localX, int y, int localZ) {
      byte[] data = raw.getData();
      if (data == null || data.length == 0) {
        return 0;
      }
      int section = y >> 4;
      int localY = y & 15;
      if ((raw.mask() & (1 << section)) == 0) {
        return 0;
      }

      int sectionsBefore = Integer.bitCount(raw.mask() & ((1 << section) - 1));
      int sectionStart = sectionsBefore * 8192;
      int blockIndex = (localY << 8) | (localZ << 4) | localX;
      int byteOffset = sectionStart + blockIndex * 2;
      if (byteOffset + 1 >= data.length) {
        return 0;
      }

      int bam = (data[byteOffset] & 0xFF) | ((data[byteOffset + 1] & 0xFF) << 8);
      return bam & 0xF;
    }
  }


  private record EntityManagerTrackedEntity(EntityManager em, int javaId, int consoleId) implements
      TrackedEntity {

    @Override
    public int javaEntityId() {
      return javaId;
    }

    @Override
    public int consoleEntityId() {
      return consoleId;
    }

    @Override
    public int typeId() {
      Integer t = em.getType(javaId);
      return t != null ? t : -1;
    }

    @Override
    public EntityCategory category() {
      EntityManager.EntityCategory cat = em.getCategory(javaId);
      if (cat == null) {
        return EntityCategory.UNKNOWN;
      }
      return switch (cat) {
        case MOB -> EntityCategory.MOB;
        case OBJECT -> EntityCategory.OBJECT;
        case PLAYER -> EntityCategory.PLAYER;
        case EXPERIENCE_ORB -> EntityCategory.EXPERIENCE_ORB;
        case PAINTING -> EntityCategory.PAINTING;
        case GLOBAL_ENTITY -> EntityCategory.GLOBAL_ENTITY;
        default -> EntityCategory.UNKNOWN;
      };
    }

    @Override
    public int rawX() {
      return em.getX(javaId);
    }

    @Override
    public int rawY() {
      return em.getY(javaId);
    }

    @Override
    public int rawZ() {
      return em.getZ(javaId);
    }

    @Override
    public byte yaw() {
      return em.getYaw(javaId);
    }

    @Override
    public byte pitch() {
      return em.getPitch(javaId);
    }
  }
}
