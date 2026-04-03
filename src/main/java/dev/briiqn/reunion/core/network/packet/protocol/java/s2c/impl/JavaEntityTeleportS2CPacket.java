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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleRotateHeadS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTeleportEntityS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x18, supports = {47})
public final class JavaEntityTeleportS2CPacket extends JavaS2CPacket {

  private int entityId, x, y, z;
  private byte yaw, pitch;

  public JavaEntityTeleportS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = VarIntUtil.read(buf);
    x = buf.readInt();
    y = buf.readInt();
    z = buf.readInt();
    yaw = buf.readByte();
    pitch = buf.readByte();
    buf.readBoolean();
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    var em = cs.getEntityManager();

    Integer cid = em.getConsoleId(entityId);
    if (cid == null) {
      return;
    }

    em.setPosition(entityId, x, y, z);
    em.setRotation(entityId, yaw, pitch);

    int cx = x;
    int cz = z;
    if (cs.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
      cx -= cs.getWorldOffsetX() * 32;
      cz -= cs.getWorldOffsetZ() * 32;
    }

    PacketManager.sendToConsole(cs,
        new ConsoleTeleportEntityS2CPacket(cid, cx, y, cz, yaw, pitch));

    Integer type = em.getType(entityId);
    if (type != null && type == 78) {
      PacketManager.sendToConsole(cs,
          new ConsoleRotateHeadS2CPacket(cid, yaw));
    }
  }
}