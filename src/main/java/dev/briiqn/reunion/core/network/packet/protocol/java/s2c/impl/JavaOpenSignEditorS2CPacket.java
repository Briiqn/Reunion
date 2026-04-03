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
import dev.briiqn.reunion.core.network.packet.data.enums.TileEditorType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTileEditorOpenS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.game.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x36, supports = {47})
public final class JavaOpenSignEditorS2CPacket extends JavaS2CPacket {

  private int x, y, z;

  public JavaOpenSignEditorS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    long pos = buf.readLong();
    this.x = BufferUtil.posX(pos);
    this.y = BufferUtil.posY(pos);
    this.z = BufferUtil.posZ(pos);
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    int cx = cs.toConsoleX(x);
    int cz = cs.toConsoleZ(z);
    PacketManager.sendToConsole(cs,
        new ConsoleTileEditorOpenS2CPacket(TileEditorType.SIGN, cx, y, cz));
  }
}