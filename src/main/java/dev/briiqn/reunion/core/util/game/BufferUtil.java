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

package dev.briiqn.reunion.core.util.game;

import io.netty.buffer.ByteBuf;


public final class BufferUtil {

  private BufferUtil() {
  }

  public static void writePosition(ByteBuf out, int x, int y, int z) {
    out.writeLong(
        ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (long) (z & 0x3FFFFFF));
  }

  public static int posX(long pos) {
    return (int) (pos >> 38);
  }

  public static int posY(long pos) {
    return (int) ((pos >> 26) & 0xFFF);
  }

  public static int posZ(long pos) {
    return (int) (pos << 38 >> 38);
  }
}
