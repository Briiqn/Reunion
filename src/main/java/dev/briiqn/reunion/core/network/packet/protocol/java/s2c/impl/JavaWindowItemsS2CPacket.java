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
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleContainerSetContentS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleContainerSetSlotS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.game.ItemUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x30, supports = {47})
public final class JavaWindowItemsS2CPacket extends JavaS2CPacket {

  private byte windowId;
  private List<ItemInstance> items;

  @Override
  public void read(ByteBuf buf) {
    windowId = buf.readByte();
    short count = buf.readShort();
    items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(ItemUtil.readJavaItem(buf));
    }
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();

    if (cs.isWindowTransitioning()) {
      cs.queueTickAction(() -> this.handle(session));
      return;
    }

    var tracker = cs.getInventoryTracker();

    if (windowId == -1) {
      if (!items.isEmpty()) {
        tracker.setCursorItem(items.get(0));
        PacketManager.sendToConsole(cs,
            new ConsoleContainerSetSlotS2CPacket((byte) -1, (short) 0, items.get(0)));
      }
      return;
    }

    tracker.setWindowId(windowId);
    tracker.setWindowSize(items.size());

    for (int i = 0; i < items.size(); i++) {
      tracker.setItem(windowId, i, items.get(i));
    }

    PacketManager.sendToConsole(cs, new ConsoleContainerSetContentS2CPacket(windowId, items));

    if (windowId != 0 && cs.getWindowType(windowId) == 4) { // Enchanting Table
      if (cs.getServer().getConfig().getGameplay().isEnchantmentTableHack()) {
        cs.queueTickAction(() -> cs.handleEnchantmentTableHack(windowId));
      }
    }
  }
}