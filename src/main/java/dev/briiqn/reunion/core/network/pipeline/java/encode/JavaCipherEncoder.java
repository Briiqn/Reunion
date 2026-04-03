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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import javax.crypto.Cipher;

public final class JavaCipherEncoder extends MessageToByteEncoder<ByteBuf> {

  private final Cipher cipher;

  public JavaCipherEncoder(Cipher cipher) {
    this.cipher = cipher;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
    int readable = in.readableBytes();
    if (readable > 0) {
      byte[] heapIn = new byte[readable];
      in.readBytes(heapIn);

      byte[] encrypted = cipher.update(heapIn);

      if (encrypted != null && encrypted.length > 0) {
        out.writeBytes(encrypted);
      }
    }
  }
}