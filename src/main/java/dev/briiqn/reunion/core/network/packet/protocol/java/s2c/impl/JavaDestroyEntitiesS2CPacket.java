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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleRemoveEntitiesS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x13, supports = {47})
public final class JavaDestroyEntitiesS2CPacket extends JavaS2CPacket {

  private List<Integer> javaIds;

  public JavaDestroyEntitiesS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    int count = VarIntUtil.read(buf);
    javaIds = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      javaIds.add(VarIntUtil.read(buf));
    }
  }

  @Override
  public void handle(JavaSession session) {
    var em = session.getConsoleSession().getEntityManager();
    List<Integer> cids = new ArrayList<>(javaIds.size());
    for (int jid : javaIds) {
      cids.add(em.map(jid));
      em.remove(jid);
    }
    if (cids.isEmpty()) {
      return;
    }

    PacketManager.sendToConsole(session.getConsoleSession(),
        new ConsoleRemoveEntitiesS2CPacket(cids));
  }
}
