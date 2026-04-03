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

public final class VarIntUtil {

  private VarIntUtil() {
  }

  public static void write(ByteBuf out, int v) {
    while ((v & ~0x7F) != 0) {
      out.writeByte((v & 0x7F) | 0x80);
      v >>>= 7;
    }
    out.writeByte(v);
  }

  public static int read(ByteBuf in) {
    int val = 0, shift = 0, b;
    do {
      b = in.readByte();
      val |= (b & 0x7F) << shift;
      shift += 7;
    } while ((b & 0x80) != 0);
    return val;
  }

  public static int len(int v) {
    int size = 0;
    while ((v & ~0x7F) != 0) {
      size++;
      v >>>= 7;
    }
    return size + 1;
  }
}
