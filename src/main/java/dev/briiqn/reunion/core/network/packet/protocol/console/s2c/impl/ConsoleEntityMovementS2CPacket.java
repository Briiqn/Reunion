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

import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import io.netty.buffer.ByteBuf;

public abstract class ConsoleEntityMovementS2CPacket extends ConsoleS2CPacket {

  protected int entityId;
  protected byte dx, dy, dz;
  protected byte yaw, pitch;
  protected boolean hasPos, hasRot;

  public ConsoleEntityMovementS2CPacket() {
  }

  public ConsoleEntityMovementS2CPacket(int entityId) {
    this.entityId = entityId;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeShort(entityId);
    if (hasPos) {
      buf.writeByte(dx);
      buf.writeByte(dy);
      buf.writeByte(dz);
    }
    if (hasRot) {
      buf.writeByte(yaw);
      buf.writeByte(pitch);
    }
  }
}