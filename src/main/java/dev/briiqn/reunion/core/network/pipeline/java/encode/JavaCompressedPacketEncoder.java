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

package dev.briiqn.reunion.core.network.pipeline.java.encode;

import static dev.briiqn.reunion.core.util.VarIntUtil.len;

import dev.briiqn.libdeflate.LibDeflate;
import dev.briiqn.reunion.core.network.packet.data.RawPacket;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class JavaCompressedPacketEncoder extends MessageToByteEncoder<RawPacket> {

  private final int threshold;
  private final int compressionLevel;

  public JavaCompressedPacketEncoder(int threshold) {
    this(threshold, 6);
  }

  public JavaCompressedPacketEncoder(int threshold, int compressionLevel) {
    this.threshold = threshold;
    this.compressionLevel = compressionLevel;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, RawPacket msg, ByteBuf out) throws Exception {
    try {
      ByteBuf raw = ctx.alloc().buffer();
      VarIntUtil.write(raw, msg.id());
      raw.writeBytes(msg.payload());

      int uncompressedSize = raw.readableBytes();
      if (uncompressedSize < threshold) {
        VarIntUtil.write(out, len(0) + uncompressedSize);
        VarIntUtil.write(out, 0);
        out.writeBytes(raw);
      } else {
        byte[] input = new byte[uncompressedSize];
        raw.readBytes(input);
        byte[] compressed = LibDeflate.compress(input, compressionLevel);
        VarIntUtil.write(out, len(uncompressedSize) + compressed.length);
        VarIntUtil.write(out, uncompressedSize);
        out.writeBytes(compressed);
      }
      raw.release();
    } finally {
      msg.payload().release();
    }
  }
}