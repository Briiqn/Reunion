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

package dev.briiqn.reunion.core.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class FourJCompressUtil {

  private FourJCompressUtil() {
  }

  public static byte[] rleEncode(byte[] input) {
    ByteBuf out = Unpooled.buffer(input.length + 1024);
    int i = 0;
    while (i < input.length) {
      byte val = input[i];
      int count = 1;
      while ((i + count) < input.length && input[i + count] == val && count < 255) {
        count++;
      }
      if (count <= 3) {
        if (val == (byte) 255) {
          out.writeByte(255);
          out.writeByte(count - 1);
        } else {
          for (int k = 0; k < count; k++) {
            out.writeByte(val);
          }
        }
      } else {
        out.writeByte(255);
        out.writeByte(count - 1);
        out.writeByte(val);
      }
      i += count;
    }
    byte[] result = new byte[out.readableBytes()];
    out.readBytes(result);
    out.release();
    return result;
  }

}
