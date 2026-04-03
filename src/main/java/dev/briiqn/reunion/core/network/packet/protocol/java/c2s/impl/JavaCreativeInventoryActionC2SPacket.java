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

@PacketInfo(side = PacketSide.JAVA_C2S, id = 0x10, supports = {47})
public final class JavaCreativeInventoryActionC2SPacket extends JavaC2SPacket {

  private final short slot;
  private final ItemInstance item;

  public JavaCreativeInventoryActionC2SPacket() {
    this((short) 0, null);
  }

  public JavaCreativeInventoryActionC2SPacket(short slot, ItemInstance item) {
    this.slot = slot;
    this.item = item;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeShort(slot);
    writeJavaItem(buf, item);
  }
}
