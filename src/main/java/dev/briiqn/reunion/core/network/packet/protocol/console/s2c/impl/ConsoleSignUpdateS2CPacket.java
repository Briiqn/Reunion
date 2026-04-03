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

package dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.util.StringUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 130, supports = {39, 78})
public final class ConsoleSignUpdateS2CPacket extends ConsoleS2CPacket {

  private final int x, z;
  private final short y;
  private final String[] lines;

  public ConsoleSignUpdateS2CPacket() {
    this(0, (short) 0, 0, new String[4]);
  }

  public ConsoleSignUpdateS2CPacket(int x, short y, int z, String[] lines) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.lines = lines;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(x);
    buf.writeShort(y);
    buf.writeInt(z);

    // m_bVerified (true = prevents console from reverifying/censoring immediately)
    buf.writeBoolean(true);
    // m_bCensored (false = show text)
    buf.writeBoolean(false);

    for (int i = 0; i < 4; i++) {
      String line = (i < lines.length && lines[i] != null) ? lines[i] : "";

      // LCE crashes if a sign line is longer than 15 characters
      StringUtil.writeConsoleUtf(buf, line, 15);
    }
  }
}