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

package dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl;

import static dev.briiqn.reunion.core.util.game.ItemUtil.writeJavaItem;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_C2S, id = 0x0E, supports = {47})
public final class JavaClickWindowC2SPacket extends JavaC2SPacket {

  private final int windowId, button, mode;
  private final short slot, actionNumber;
  private final ItemInstance item;

  public JavaClickWindowC2SPacket() {
    this(0, (short) 0, 0, (short) 0, 0, null);
  }

  public JavaClickWindowC2SPacket(int windowId, short slot, int button, short actionNumber,
      int mode, ItemInstance item) {
    this.windowId = windowId;
    this.slot = slot;
    this.button = button;
    this.actionNumber = actionNumber;
    this.mode = mode;
    this.item = item;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeByte(windowId);
    buf.writeShort(slot);
    buf.writeByte(button);
    buf.writeShort(actionNumber);
    buf.writeByte(mode);
    writeJavaItem(buf, item);
  }
}
