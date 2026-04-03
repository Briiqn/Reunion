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

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.util.StringUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 100, supports = {39, 78})
public final class ConsoleContainerOpenS2CPacket extends ConsoleS2CPacket {

  private final byte windowId;
  private final int invType;
  private final short slots;
  private final String title;
  private final int entityId;

  public ConsoleContainerOpenS2CPacket() {
    this((byte) 0, 0, (short) 9, "", 0);
  }

  public ConsoleContainerOpenS2CPacket(byte windowId, int invType, short slots, String title,
      int entityId) {
    this.windowId = windowId;
    this.invType = invType;
    this.slots = slots;
    this.title = title;
    this.entityId = entityId;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeByte(windowId);
    buf.writeByte(invType);
    buf.writeByte(slots);

    String safeTitle = title == null ? "" : title;
    boolean hasTitle = !safeTitle.isEmpty();

    if (safeTitle.startsWith("container.") || safeTitle.startsWith("entity.")) {
      hasTitle = false;
    }

    buf.writeBoolean(hasTitle);

    if (invType == 11) { // HORSE
      buf.writeInt(entityId);
    }

    if (hasTitle) {
      StringUtil.writeConsoleUtf(buf, safeTitle, 64);
    }
  }
}