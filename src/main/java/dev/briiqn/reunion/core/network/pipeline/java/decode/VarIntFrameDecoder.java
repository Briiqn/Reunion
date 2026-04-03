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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;


public final class VarIntFrameDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    in.markReaderIndex();
    int val = 0, shift = 0, b;
    for (int i = 0; i < 5; i++) {
      if (!in.isReadable()) {
        in.resetReaderIndex();
        return;
      }
      b = in.readByte();
      val |= (b & 0x7F) << shift;
      shift += 7;
      if ((b & 0x80) == 0) {
        if (in.readableBytes() < val) {
          in.resetReaderIndex();
          return;
        }
        out.add(in.readRetainedSlice(val));
        return;
      }
    }
    throw new CorruptedFrameException("VarInt too big");
  }
}
