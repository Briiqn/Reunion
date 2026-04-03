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
import dev.briiqn.reunion.core.network.packet.data.enums.EntityLinkType;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 39, supports = {39, 78})
public final class ConsoleSetRidingS2CPacket extends ConsoleS2CPacket {

  private final int entityId;
  private final int vehicleId;

  public ConsoleSetRidingS2CPacket() {
    this(0, -1);
  }

  public ConsoleSetRidingS2CPacket(int entityId, int vehicleId) {
    this.entityId = entityId;
    this.vehicleId = vehicleId;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(entityId);
    buf.writeInt(vehicleId);
    buf.writeByte(EntityLinkType.RIDING.getId());
  }

  public void write39(ByteBuf buf) {
    buf.writeInt(entityId);
    buf.writeInt(vehicleId);
  }
}