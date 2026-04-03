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

import static dev.briiqn.reunion.core.util.VarIntUtil.read;
import static dev.briiqn.reunion.core.util.VarIntUtil.write;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public final class StringUtil {

  private StringUtil() {
  }

  public static String readConsoleUtf(ByteBuf buf) {
    int len = buf.readUnsignedShort();
    byte[] b = new byte[len * 2];
    buf.readBytes(b);
    return new String(b, StandardCharsets.UTF_16BE);
  }

  public static void writeConsoleUtf(ByteBuf buf, String s, int maxChars) {
    if (s == null) {
      s = "";
    }
    if (s.length() > maxChars) {
      s = s.substring(0, maxChars);
    }
    buf.writeShort(s.length());
    buf.writeCharSequence(s, StandardCharsets.UTF_16BE);
  }

  public static void writeConsoleModifiedUtf8(ByteBuf buf, String s) {
    if (s == null) {
      s = "";
    }
    byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    buf.writeShort(bytes.length);
    buf.writeBytes(bytes);
  }

  public static String readConsoleModifiedUtf8(ByteBuf buf) {
    int utflen = buf.readUnsignedShort();
    byte[] bytearr = new byte[utflen];
    buf.readBytes(bytearr);
    return new String(bytearr, StandardCharsets.UTF_8);
  }

  public static String readJavaString(ByteBuf buf) {
    int len = read(buf);
    byte[] b = new byte[len];
    buf.readBytes(b);
    return new String(b, StandardCharsets.UTF_8);
  }

  public static void writeJavaString(ByteBuf buf, String s) {
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    write(buf, b.length);
    buf.writeBytes(b);
  }

}