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
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaKeepAliveC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 0, supports = {39, 78})
public final class ConsoleKeepAliveC2SPacket extends ConsoleC2SPacket {

  private int keepAliveId;

  public ConsoleKeepAliveC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    keepAliveId = buf.readInt();
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(keepAliveId);
  }

  @Override
  public void handle(ConsoleSession session) {
    session.onKeepAliveReceived();

    PacketManager.sendToJava(session.getJavaSession(), new JavaKeepAliveC2SPacket(keepAliveId));
  }
}