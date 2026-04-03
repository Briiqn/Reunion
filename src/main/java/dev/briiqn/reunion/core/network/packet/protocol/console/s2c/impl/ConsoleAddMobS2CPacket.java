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

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 24, supports = {39, 78})
public final class ConsoleAddMobS2CPacket extends ConsoleS2CPacket {

  private final int consoleId;
  private final int type;
  private final int x, y, z;
  private final byte yaw, pitch, headYaw;
  private final short vx, vy, vz;
  private final byte metaFlags;

  public ConsoleAddMobS2CPacket() {
    this(0, 54, 0, 0, 0, (byte) 0, (byte) 0, (byte) 0, (short) 0, (short) 0, (short) 0, (byte) 0);
  }

  public ConsoleAddMobS2CPacket(int consoleId, int type, int x, int y, int z,
      byte yaw, byte pitch, byte headYaw, short vx, short vy, short vz, byte metaFlags) {
    this.consoleId = consoleId;
    this.type = type;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.headYaw = headYaw;
    this.vx = vx;
    this.vy = vy;
    this.vz = vz;
    this.metaFlags = metaFlags;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeShort(consoleId);
    buf.writeByte(type);
    buf.writeInt(x);
    buf.writeInt(y);
    buf.writeInt(z);
    buf.writeByte(yaw);
    buf.writeByte(pitch);
    buf.writeByte(headYaw); // yHeadRot
    buf.writeShort(vx);
    buf.writeShort(vy);
    buf.writeShort(vz);
    buf.writeByte(0);
    buf.writeByte(metaFlags & 0x20);
    buf.writeByte(127);
  }
}