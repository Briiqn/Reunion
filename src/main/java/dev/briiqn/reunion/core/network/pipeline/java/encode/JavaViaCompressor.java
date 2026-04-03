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

import dev.briiqn.libdeflate.LibDeflate;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class JavaViaCompressor extends MessageToByteEncoder<ByteBuf> {

  private final int threshold;

  public JavaViaCompressor(int threshold) {
    this.threshold = threshold;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
    int uncompressedLen = msg.readableBytes();
    if (uncompressedLen < threshold) {
      VarIntUtil.write(out, 0);        // data_length = 0 (uncompressed)
      out.writeBytes(msg);
    } else {
      byte[] input = new byte[uncompressedLen];
      msg.readBytes(input);
      byte[] compressed = LibDeflate.compress(input, 6);
      VarIntUtil.write(out, uncompressedLen);   // data_length
      out.writeBytes(compressed);
    }
  }
}