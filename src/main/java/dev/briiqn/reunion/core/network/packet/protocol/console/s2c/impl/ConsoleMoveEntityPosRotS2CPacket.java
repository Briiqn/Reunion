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

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 33, supports = {39, 78})
public final class ConsoleMoveEntityPosRotS2CPacket extends ConsoleEntityMovementS2CPacket {

  public ConsoleMoveEntityPosRotS2CPacket() {
    this.hasPos = true;
    this.hasRot = true;
  }

  public ConsoleMoveEntityPosRotS2CPacket(int entityId, byte dx, byte dy, byte dz, byte yaw,
      byte pitch) {
    super(entityId);
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.yaw = yaw;
    this.pitch = pitch;
    this.hasPos = true;
    this.hasRot = true;
  }
}