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
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaClickWindowC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 102, supports = {39, 78})
public final class ConsoleContainerClickC2SPacket extends ConsoleC2SPacket {

  private int containerId, button;
  private short slot, uid;
  private int clickType;
  private ItemInstance item;

  public ConsoleContainerClickC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    containerId = buf.readByte();
    slot = buf.readShort();
    button = buf.readByte();
    uid = buf.readShort();
    clickType = buf.readByte();
    item = readConsoleItem(buf);
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    int javaWindowId = session.getInventoryTracker().getWindowId();

    int targetWindow = (containerId == 0) ? javaWindowId : containerId;

    PacketManager.sendToJava(session.getJavaSession(),
        new JavaClickWindowC2SPacket(
            targetWindow,
            slot,
            button,
            session.nextActionId(),
            clickType,
            item
        ));
  }
}