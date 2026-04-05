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

package dev.briiqn.reunion.core.network.packet.protocol.console.c2s.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 131, supports = {39, 78})
public final class ConsoleComplexItemDataC2SPacket extends ConsoleC2SPacket {

  private short itemType;
  private short itemId;
  private byte[] data;

  public ConsoleComplexItemDataC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    itemType = buf.readShort();
    itemId = buf.readShort();
    int len = buf.readUnsignedShort();
    
    if (len > 32767) {
      len = 0;
    }

    if (len > 0) {
      data = new byte[len];
      buf.readBytes(data);
    } else {
      data = new byte[0];
    }
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeShort(itemType);
    buf.writeShort(itemId);
    buf.writeShort(data.length);
    if (data.length > 0) {
      buf.writeBytes(data);
    }
  }

  @Override
  public void handle(ConsoleSession session) {
  }
}