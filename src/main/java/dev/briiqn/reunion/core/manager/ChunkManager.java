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

package dev.briiqn.reunion.core.manager;

import static dev.briiqn.reunion.core.util.FourJCompressUtil.rleEncode;
import static dev.briiqn.reunion.core.util.VarIntUtil.read;

import dev.briiqn.libdeflate.LibDeflate;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleBlockRegionUpdateS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleChunkVisibilityS2CPacket;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ChunkManager {

  private static final int HEIGHT = 256;
  private static final int SECTIONS = HEIGHT / 16;

  private final ConsoleSession session;
  private final Map<Long, CachedChunk> chunkCache = new ConcurrentHashMap<>();

  public ChunkManager(ConsoleSession session) {
    this.session = session;
  }

  public static long getChunkKey(int x, int z) {
    return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
  }

  public void clearCache() {
    for (CachedChunk c : chunkCache.values()) {
      c.cleanup();
    }
    chunkCache.clear();
  }

  public void unloadAllChunks() {
    for (CachedChunk c : chunkCache.values()) {
      int oldConsoleX = c.x() - (session.getWorldOffsetX() >> 4);
      int oldConsoleZ = c.z() - (session.getWorldOffsetZ() >> 4);
      PacketManager.sendToConsole(session,
          new ConsoleChunkVisibilityS2CPacket(oldConsoleX, oldConsoleZ, false));
    }
  }

  public void resendAllChunks() {
    for (CachedChunk c : chunkCache.values()) {
      int consoleChunkX = c.x() - (session.getWorldOffsetX() >> 4);
      int consoleChunkZ = c.z() - (session.getWorldOffsetZ() >> 4);
      byte[] chunkData = c.getData();
      if (chunkData == null || chunkData.length == 0) {
        continue;
      }

      ByteBuf buf = Unpooled.wrappedBuffer(chunkData);
      processChunk(consoleChunkX, consoleChunkZ, c.mask(), c.fullChunk(), c.hasSky(), buf, null,
          c.dimension());
      buf.release();
    }
  }

  public CachedChunk getCachedChunk(int chunkX, int chunkZ) {
    return chunkCache.get(getChunkKey(chunkX, chunkZ));
  }

  public Collection<CachedChunk> getAllCachedChunks() {
    return Collections.unmodifiableCollection(chunkCache.values());
  }

  public int getCacheSize() {
    return chunkCache.size();
  }

  private void cacheChunk(int chunkX, int chunkZ, int mask, boolean fullChunk, boolean hasSky,
      byte[] dataBytes, int dimension) {
    long key = getChunkKey(chunkX, chunkZ);
    File diskFile = null;
    Arena arena = null;
    MemorySegment memorySegment = null;

    if (session.getServer().getConfig().getGameplay().isOffloadChunksToDisk()) {
      File dir = new File("saves", "chunks");
      if (!dir.exists()) {
        dir.mkdirs();
      }

      diskFile = new File(dir, session.getPlayerName() + "_" + key + ".bin");
      try {
        byte[] compressed = LibDeflate.compress(dataBytes, 6);
        ByteBuffer buf = ByteBuffer.allocate(4 + compressed.length);
        buf.putInt(dataBytes.length);
        buf.put(compressed);
        Files.write(diskFile.toPath(), buf.array());
        diskFile.deleteOnExit();
      } catch (Exception e) {
        log.error("Failed to compress/offload chunk to disk", e);
        diskFile = null;
      }
    }

    if (diskFile == null) {
      arena = Arena.ofShared();
      memorySegment = arena.allocate(dataBytes.length);
      MemorySegment.copy(MemorySegment.ofArray(dataBytes), 0, memorySegment, 0, dataBytes.length);
    }

    CachedChunk newChunk = new CachedChunk(chunkX, chunkZ, mask, fullChunk, hasSky,
        arena, memorySegment, dataBytes.length, dimension, diskFile);

    CachedChunk oldChunk = chunkCache.put(key, newChunk);

    if (oldChunk != null) {
      oldChunk.cleanup();
    }
  }


  public void handleChunkData(ByteBuf in) {
    int chunkX = in.readInt();
    int chunkZ = in.readInt();

    if (session.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
      int currentConsoleX = chunkX - (session.getWorldOffsetX() >> 4);
      int currentConsoleZ = chunkZ - (session.getWorldOffsetZ() >> 4);

      int chunkThreshold = session.getHackThreshold() >> 4;
      if (Math.abs(currentConsoleX) > chunkThreshold
          || Math.abs(currentConsoleZ) > chunkThreshold) {
        session.checkWorldBounds(chunkX * 16.0, chunkZ * 16.0);
      }
    }

    boolean fullChunk = in.readBoolean();
    int primaryBitMask = in.readUnsignedShort();
    int size = read(in);
    ByteBuf javaData = in.readRetainedSlice(size);

    int sectionCount = Integer.bitCount(primaryBitMask);
    boolean hasSky = sectionCount == 0 || ((size - (fullChunk ? 256 : 0)) / sectionCount == 12288);

    int dimension = session.getClientVersion() >= 78 ? session.getDimension() : -1;

    if (fullChunk && primaryBitMask == 0) {
      CachedChunk removed = chunkCache.remove(getChunkKey(chunkX, chunkZ));
      if (removed != null) {
        removed.cleanup();
      }

      int consoleChunkX = chunkX;
      int consoleChunkZ = chunkZ;
      if (session.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
        consoleChunkX = chunkX - (session.getWorldOffsetX() >> 4);
        consoleChunkZ = chunkZ - (session.getWorldOffsetZ() >> 4);
      }
      PacketManager.sendToConsole(session,
          new ConsoleChunkVisibilityS2CPacket(consoleChunkX, consoleChunkZ, false));
      javaData.release();
      return;
    } else {
      byte[] dataBytes = new byte[javaData.readableBytes()];
      javaData.getBytes(javaData.readerIndex(), dataBytes);
      cacheChunk(chunkX, chunkZ, primaryBitMask, fullChunk, hasSky, dataBytes, dimension);
    }

    int consoleChunkX = chunkX;
    int consoleChunkZ = chunkZ;

    if (session.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
      consoleChunkX = chunkX - (session.getWorldOffsetX() >> 4);
      consoleChunkZ = chunkZ - (session.getWorldOffsetZ() >> 4);
    }

    processChunk(consoleChunkX, consoleChunkZ, primaryBitMask, fullChunk, hasSky, javaData, null,
        dimension);
    javaData.release();
  }

  public void handleMapChunkBulk(ByteBuf in) {
    boolean skyLightSent = in.readBoolean();
    int chunkCount = read(in);
    int[] x = new int[chunkCount];
    int[] z = new int[chunkCount];
    int[] mask = new int[chunkCount];

    int chunkThreshold = session.getHackThreshold() >> 4;

    for (int i = 0; i < chunkCount; i++) {
      x[i] = in.readInt();
      z[i] = in.readInt();
      mask[i] = in.readUnsignedShort();

      if (session.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
        int currentConsoleX = x[i] - (session.getWorldOffsetX() >> 4);
        int currentConsoleZ = z[i] - (session.getWorldOffsetZ() >> 4);
        if (Math.abs(currentConsoleX) > chunkThreshold
            || Math.abs(currentConsoleZ) > chunkThreshold) {
          session.checkWorldBounds(x[i] * 16.0, z[i] * 16.0);
        }
      }
    }
    for (int i = 0; i < chunkCount; i++) {
      int sections = Integer.bitCount(mask[i]);
      int dataSize = sections * (8192 + 2048 + (skyLightSent ? 2048 : 0)) + 256;
      if (in.readableBytes() < dataSize) {
        break;
      }

      ByteBuf chunkData = in.readRetainedSlice(dataSize);

      byte[] dataBytes = new byte[chunkData.readableBytes()];
      chunkData.getBytes(chunkData.readerIndex(), dataBytes);
      cacheChunk(x[i], z[i], mask[i], true, skyLightSent, dataBytes, session.getDimension());

      int consoleChunkX = x[i];
      int consoleChunkZ = z[i];

      if (session.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
        consoleChunkX = x[i] - (session.getWorldOffsetX() >> 4);
        consoleChunkZ = z[i] - (session.getWorldOffsetZ() >> 4);
      }

      processChunk(consoleChunkX, consoleChunkZ, mask[i], true, skyLightSent, chunkData, null,
          session.getDimension());
      chunkData.release();
    }
  }

  private void processChunk(int chunkX, int chunkZ, int mask, boolean fullChunk,
      boolean hasSky, ByteBuf javaData, ByteBuf bulkBiomes, int dimension) {
    try {
      if (fullChunk) {
        PacketManager.sendToConsole(session,
            new ConsoleChunkVisibilityS2CPacket(chunkX, chunkZ, true));
      }

      BlockRegistry blocks = BlockRegistry.getInstance();

      byte[][] jBlocks = new byte[SECTIONS][4096];
      byte[][] jMeta = new byte[SECTIONS][2048];
      byte[][] jBL = new byte[SECTIONS][2048];
      byte[][] jSL = new byte[SECTIONS][2048];

      for (int s = 0; s < SECTIONS; s++) {
        if ((mask & (1 << s)) == 0) {
          continue;
        }
        for (int i = 0; i < 4096; i++) {
          int bam = javaData.readUnsignedByte() | (javaData.readUnsignedByte() << 8);
          int[] remapped = blocks.remapBlock(bam >> 4, bam & 0xF);
          int id = remapped[0];
          int m = remapped[1];

          jBlocks[s][i] = (byte) id;
          int byteIdx = i >> 1;
          if ((i & 1) == 0) {
            jMeta[s][byteIdx] = (byte) ((jMeta[s][byteIdx] & 0xF0) | m);
          } else {
            jMeta[s][byteIdx] = (byte) ((jMeta[s][byteIdx] & 0x0F) | (m << 4));
          }
        }
      }
      for (int s = 0; s < SECTIONS; s++) {
        if ((mask & (1 << s)) != 0) {
          javaData.readBytes(jBL[s]);
        }
      }
      if (hasSky) {
        for (int s = 0; s < SECTIONS; s++) {
          if ((mask & (1 << s)) != 0) {
            javaData.readBytes(jSL[s]);
          }
        }
      }

      byte[] biomes = new byte[256];
      if (fullChunk) {
        if (bulkBiomes != null) {
          bulkBiomes.readBytes(biomes);
        } else if (javaData.readableBytes() >= 256) {
          javaData.readBytes(biomes);
        }
      }

      if (fullChunk) {
        byte[] rawBlocks = new byte[16 * 16 * HEIGHT];
        byte[] meta = new byte[16 * 16 * HEIGHT / 2];
        byte[] bl = new byte[16 * 16 * HEIGHT / 2];
        byte[] sl = new byte[16 * 16 * HEIGHT / 2];
        Arrays.fill(sl, (byte) 0xFF);

        for (int s = 0; s < SECTIONS; s++) {
          if ((mask & (1 << s)) == 0) {
            continue;
          }
          System.arraycopy(jBlocks[s], 0, rawBlocks, s * 4096, 4096);
          System.arraycopy(jMeta[s], 0, meta, s * 2048, 2048);
          System.arraycopy(jBL[s], 0, bl, s * 2048, 2048);
          if (hasSky) {
            System.arraycopy(jSL[s], 0, sl, s * 2048, 2048);
          }
        }

        byte[] rMeta = new byte[16 * 16 * HEIGHT / 2];
        byte[] rBL = new byte[16 * 16 * HEIGHT / 2];
        byte[] rSL = new byte[16 * 16 * HEIGHT / 2];

        for (int ry = 0; ry < HEIGHT; ry++) {
          boolean isUpper = ry >= 128;
          int localY = ry % 128;
          int sectionOffset = isUpper ? 16384 : 0;
          int shift = (localY & 1) * 4;

          for (int rz = 0; rz < 16; rz++) {
            for (int rx = 0; rx < 16; rx++) {
              int src = ry * 256 + rz * 16 + rx;
              int dstHalf = sectionOffset + (rx * 1024) + (rz * 64) + (localY >> 1);

              rMeta[dstHalf] |= (byte) (((meta[src >> 1] >> ((src & 1) * 4)) & 0xF) << shift);
              rBL[dstHalf] |= (byte) (((bl[src >> 1] >> ((src & 1) * 4)) & 0xF) << shift);
              rSL[dstHalf] |= (byte) (((sl[src >> 1] >> ((src & 1) * 4)) & 0xF) << shift);
            }
          }
        }

        ByteBuf raw = Unpooled.buffer();
        raw.writeBytes(rawBlocks);
        raw.writeBytes(rMeta);
        raw.writeBytes(rBL);
        raw.writeBytes(rSL);
        raw.writeBytes(biomes);
        byte[] flat = new byte[raw.readableBytes()];
        raw.readBytes(flat);
        raw.release();

        PacketManager.sendToConsole(session, new ConsoleBlockRegionUpdateS2CPacket(
            true, chunkX * 16, 0, chunkZ * 16,
            15, HEIGHT - 1, 15, LibDeflate.compress(rleEncode(flat), 6), dimension));

      } else {
        for (int s = 0; s < SECTIONS; s++) {
          if ((mask & (1 << s)) == 0) {
            continue;
          }

          byte[] pBlocks = new byte[4096];
          byte[] pMeta = new byte[2048];
          byte[] pBL = new byte[2048];
          byte[] pSL = new byte[2048];
          Arrays.fill(pSL, (byte) 0xFF);

          for (int xi = 0; xi < 16; xi++) {
            for (int zi = 0; zi < 16; zi++) {
              for (int yi = 0; yi < 16; yi++) {
                int srcIdx = (yi << 8) | (zi << 4) | xi;
                int dstIdx = (xi << 8) | (zi << 4) | yi;
                int srcHalf = srcIdx >> 1;
                int dstHalf = dstIdx >> 1;
                int dstShift = (dstIdx & 1) * 4;

                pBlocks[dstIdx] = jBlocks[s][srcIdx];
                pMeta[dstHalf] |= (byte) (((jMeta[s][srcHalf] >> ((srcIdx & 1) * 4)) & 0xF)
                    << dstShift);
                pBL[dstHalf] |= (byte) (((jBL[s][srcHalf] >> ((srcIdx & 1) * 4)) & 0xF)
                    << dstShift);
                if (hasSky) {
                  int slv = (jSL[s][srcHalf] >> ((srcIdx & 1) * 4)) & 0xF;
                  pSL[dstHalf] &= (byte) ~(0xF << dstShift);
                  pSL[dstHalf] |= (byte) (slv << dstShift);
                }
              }
            }
          }

          ByteBuf raw = Unpooled.buffer();
          raw.writeBytes(pBlocks);
          raw.writeBytes(pMeta);
          raw.writeBytes(pBL);
          raw.writeBytes(pSL);
          byte[] flat = new byte[raw.readableBytes()];
          raw.readBytes(flat);
          raw.release();

          PacketManager.sendToConsole(session, new ConsoleBlockRegionUpdateS2CPacket(
              false, chunkX * 16, s * 16, chunkZ * 16,
              15, 15, 15, LibDeflate.compress(rleEncode(flat), 6), dimension));
        }
      }
    } catch (Exception e) {
      log.error("[ChunkManager] Error processing chunk ({}, {}): {}", chunkX, chunkZ,
          e.getMessage(), e);
    }
  }

  public record CachedChunk(int x, int z, int mask, boolean fullChunk, boolean hasSky,
                            Arena arena, MemorySegment segment, int length, int dimension,
                            File diskFile) {

    public byte[] getData() {
      if (segment != null) {
        return segment.toArray(ValueLayout.JAVA_BYTE);
      }
      if (diskFile != null && diskFile.exists()) {
        try {
          byte[] fileData = Files.readAllBytes(diskFile.toPath());
          ByteBuffer buf = ByteBuffer.wrap(fileData);
          int uncompressedSize = buf.getInt();
          byte[] compressed = new byte[buf.remaining()];
          buf.get(compressed);
          return LibDeflate.decompress(compressed, uncompressedSize);
        } catch (Exception e) {
          log.error("Failed to read/decompress offloaded chunk from disk", e);
        }
      }
      return new byte[0];
    }

    public void cleanup() {
      if (arena != null) {
        try {
          arena.close();
        } catch (Exception ignored) {
        }
      }
      if (diskFile != null && diskFile.exists()) {
        diskFile.delete();
      }
    }
  }
}