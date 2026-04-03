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

package dev.briiqn.reunion.core.network.packet.protocol.java.s2c.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleEntityEventS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x1A, supports = {47})
public final class JavaEntityStatusS2CPacket extends JavaS2CPacket {

  private int entityId;
  private byte status;

  public JavaEntityStatusS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = buf.readInt();
    status = buf.readByte();
  }

  @Override
  public void handle(JavaSession session) {
    Integer cid = session.getConsoleSession().getEntityManager().getConsoleId(entityId);
    if (cid == null) {
      return;
    }

    PacketManager.sendToConsole(session.getConsoleSession(),
        new ConsoleEntityEventS2CPacket(cid, status));
  }
}