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

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 32, supports = {39, 78})
public final class ConsoleMoveEntityRotS2CPacket extends ConsoleEntityMovementS2CPacket {

  public ConsoleMoveEntityRotS2CPacket() {
    this.hasPos = false;
    this.hasRot = true;
  }

  public ConsoleMoveEntityRotS2CPacket(int entityId, byte yaw, byte pitch) {
    super(entityId);
    this.yaw = yaw;
    this.pitch = pitch;
    this.hasPos = false;
    this.hasRot = true;
  }
}