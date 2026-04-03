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

import static dev.briiqn.reunion.core.util.game.ItemUtil.readConsoleItem;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaPlayerBlockPlacementC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.game.ItemUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 15, supports = {39, 78})
public final class ConsoleUseItemC2SPacket extends ConsoleC2SPacket {

  private int x, y, z, face, clickX, clickY, clickZ;
  private ItemInstance item;

  public ConsoleUseItemC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    x = buf.readInt();
    y = buf.readUnsignedByte();
    z = buf.readInt();
    face = buf.readUnsignedByte();
    item = readConsoleItem(buf);
    clickX = buf.readUnsignedByte();
    clickY = buf.readUnsignedByte();
    clickZ = buf.readUnsignedByte();
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    if (ItemUtil.isUsable(item)) {
      session.setUsingItem(true);
      session.setLastUsedItem(item);
    } else {
      session.setUsingItem(false);
      session.setNeedsReblock(false);
    }
    // remap to what servers expect from an actual 1.8 client
    if (x == -1 && y == 255 && z == -1) {
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaPlayerBlockPlacementC2SPacket(-1, -1, -1, 255, item, 0, 0, 0));
      return;
    }

    int javaX = session.toJavaX(x);
    int javaZ = session.toJavaZ(z);

    PacketManager.sendToJava(session.getJavaSession(),
        new JavaPlayerBlockPlacementC2SPacket(javaX, y, javaZ, face, item, clickX, clickY, clickZ));
  }
}