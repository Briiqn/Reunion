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

package dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl;

import static dev.briiqn.reunion.core.util.game.ItemUtil.writeConsoleItemAuto;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import io.netty.buffer.ByteBuf;
import java.util.List;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 104, supports = {39, 78})
public final class ConsoleContainerSetContentS2CPacket extends ConsoleS2CPacket {

  private final byte windowId;
  private final List<ItemInstance> items;

  public ConsoleContainerSetContentS2CPacket() {
    this((byte) 0, List.of());
  }

  public ConsoleContainerSetContentS2CPacket(byte windowId, List<ItemInstance> items) {
    this.windowId = windowId;
    this.items = items;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeByte(windowId);
    buf.writeShort(items.size());
    for (ItemInstance item : items) {
      writeConsoleItemAuto(buf, item);
    }
  }
}
