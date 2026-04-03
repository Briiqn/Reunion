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

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 28, supports = {39, 78})
public final class ConsoleSetEntityMotionS2CPacket extends ConsoleS2CPacket {

  private final int entityId;
  private final short vx, vy, vz;

  public ConsoleSetEntityMotionS2CPacket() {
    this(0, (short) 0, (short) 0, (short) 0);
  }

  public ConsoleSetEntityMotionS2CPacket(int entityId, short vx, short vy, short vz) {
    this.entityId = entityId;
    this.vx = vx;
    this.vy = vy;
    this.vz = vz;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeShort(entityId);
    buf.writeShort(vx);
    buf.writeShort(vy);
    buf.writeShort(vz);
  }
}
