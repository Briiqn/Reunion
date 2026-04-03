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

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 17, supports = {39, 78})
public final class ConsoleEntityActionAtPositionS2CPacket extends ConsoleS2CPacket {

  private final int entityId;
  private final int action;
  private final int x;
  private final int y;
  private final int z;

  public ConsoleEntityActionAtPositionS2CPacket() {
    this(0, 0, 0, 0, 0);
  }

  public ConsoleEntityActionAtPositionS2CPacket(int entityId, int action, int x, int y, int z) {
    this.entityId = entityId;
    this.action = action;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(entityId);
    buf.writeByte(action);
    buf.writeInt(x);
    buf.writeByte(y);
    buf.writeInt(z);
  }
}
