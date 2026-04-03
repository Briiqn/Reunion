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
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import javax.crypto.Cipher;

public final class JavaCipherDecoder extends MessageToMessageDecoder<ByteBuf> {

  private final Cipher cipher;

  public JavaCipherDecoder(Cipher cipher) {
    this.cipher = cipher;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    int readable = in.readableBytes();
    if (readable > 0) {
      byte[] heapIn = new byte[readable];
      in.readBytes(heapIn);
      byte[] decrypted = cipher.update(heapIn);
      if (decrypted != null && decrypted.length > 0) {
        out.add(ctx.alloc().buffer(decrypted.length).writeBytes(decrypted));
      }
    }
  }
}