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

package dev.briiqn.reunion.core.network.pipeline.java.decode;

import static dev.briiqn.reunion.core.util.VarIntUtil.read;

import dev.briiqn.libdeflate.LibDeflate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

public final class JavaViaDecompressor extends MessageToMessageDecoder<ByteBuf> {

  private final int threshold;

  public JavaViaDecompressor(int threshold) {
    this.threshold = threshold;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (!in.isReadable()) {
      return;
    }
    int uncompressedLen = read(in);
    if (uncompressedLen == 0) {
      out.add(in.readRetainedSlice(in.readableBytes()));
    } else {
      byte[] compressed = new byte[in.readableBytes()];
      in.readBytes(compressed);
      out.add(Unpooled.wrappedBuffer(LibDeflate.decompress(compressed, uncompressedLen)));
    }
  }
}