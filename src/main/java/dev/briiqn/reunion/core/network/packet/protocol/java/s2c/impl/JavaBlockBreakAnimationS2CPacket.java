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

import static dev.briiqn.reunion.core.util.game.BufferUtil.posX;
import static dev.briiqn.reunion.core.util.game.BufferUtil.posY;
import static dev.briiqn.reunion.core.util.game.BufferUtil.posZ;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTileDestructionS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x25, supports = {47})
public final class JavaBlockBreakAnimationS2CPacket extends JavaS2CPacket {

  private int entityId;
  private long pos;
  private int stage;

  public JavaBlockBreakAnimationS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = VarIntUtil.read(buf);
    pos = buf.readLong();
    stage = buf.readByte();
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    Integer cid = cs.getEntityManager().getConsoleId(entityId);
    if (cid == null) {
      return;
    }

    int cx = cs.toConsoleX(posX(pos));
    int cz = cs.toConsoleZ(posZ(pos));
    PacketManager.sendToConsole(cs,
        new ConsoleTileDestructionS2CPacket(cid, cx, posY(pos), cz, stage));
  }
}