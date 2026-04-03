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
package dev.briiqn.reunion.core.world;

import dev.briiqn.reunion.api.world.BlockBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;

public class SimpleBlockBuffer implements BlockBuffer {

  private final int width;
  private final int height;
  private final int depth;
  private final byte[] blocks = new byte[4096];
  private final byte[] metadata = new byte[2048];

  public SimpleBlockBuffer(int width, int height, int depth) {
    if (width <= 0 || height <= 0 || depth <= 0 || width > 16 || height > 16 || depth > 16) {
      throw new IllegalArgumentException(
          "Invalid buffer dimensions: " + width + "x" + height + "x" + depth);
    }
    this.width = width;
    this.height = height;
    this.depth = depth;
  }

  @Override
  public void setBlock(int x, int y, int z, int id, int meta) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
      return;
    }
    int index = (x << 8) | (z << 4) | y;
    blocks[index] = (byte) id;
    int half = index >> 1;
    if ((index & 1) == 0) {
      metadata[half] = (byte) ((metadata[half] & 0xF0) | (meta & 0x0F));
    } else {
      metadata[half] = (byte) ((metadata[half] & 0x0F) | ((meta & 0x0F) << 4));
    }
  }

  public int getPacketWidth() {
    return 15;
  }

  public int getPacketHeight() {
    return 15;
  }

  public int getPacketDepth() {
    return 15;
  }

  public byte[] getLCEData() {
    byte[] blockLight = new byte[2048];
    byte[] skyLight = new byte[2048];
    Arrays.fill(skyLight, (byte) 0xFF);
    ByteBuf flat = Unpooled.buffer(10240);
    flat.writeBytes(blocks);
    flat.writeBytes(metadata);
    flat.writeBytes(blockLight);
    flat.writeBytes(skyLight);
    byte[] raw = new byte[flat.readableBytes()];
    flat.readBytes(raw);
    flat.release();
    return raw;
  }
}