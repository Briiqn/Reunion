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
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 61, supports = {39, 78})
public final class ConsoleLevelEventS2CPacket extends ConsoleS2CPacket {

  private final int effectId, x, y, z, data;
  private final boolean globalEvent;

  public ConsoleLevelEventS2CPacket() {
    this(0, 0, 0, 0, 0, false);
  }

  public ConsoleLevelEventS2CPacket(int effectId, int x, int y, int z, int data,
      boolean globalEvent) {
    this.effectId = effectId;
    this.x = x;
    this.y = y;
    this.z = z;
    this.data = data;
    this.globalEvent = globalEvent;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(effectId);
    buf.writeInt(x);
    buf.writeByte(y & 0xFF);
    buf.writeInt(z);
    buf.writeInt(data);
    buf.writeBoolean(globalEvent);
  }

  public void write39(ByteBuf buf) {
    buf.writeInt(effectId);
    buf.writeInt(x);
    buf.writeByte(y & 0xFF);
    buf.writeInt(z);
    buf.writeInt(data);
  }
}