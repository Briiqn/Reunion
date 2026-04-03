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

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 51, supports = {39, 78})
public final class ConsoleBlockRegionUpdateS2CPacket extends ConsoleS2CPacket {

  private final boolean fullChunk;
  private final int x, y, z;
  private final int xSize, ySize, zSize;
  private final byte[] data;
  private final int dimension; // ADDED

  public ConsoleBlockRegionUpdateS2CPacket() {
    this(false, 0, 0, 0, 0, 0, 0, new byte[0], 0);
  }

  public ConsoleBlockRegionUpdateS2CPacket(boolean fullChunk, int x, int y, int z,
      int xSize, int ySize, int zSize, byte[] data, int dimension) {
    this.fullChunk = fullChunk;
    this.x = x;
    this.y = y;
    this.z = z;
    this.xSize = xSize;
    this.ySize = ySize;
    this.zSize = zSize;
    this.data = data;
    this.dimension = dimension;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeBoolean(fullChunk);
    buf.writeInt(x);
    buf.writeShort(y);
    buf.writeInt(z);
    buf.writeByte(xSize);
    buf.writeByte(ySize);
    buf.writeByte(zSize);

    int levelIdx = (dimension == 0) ? 0 : (dimension == -1 ? 1 : 2);

    buf.writeInt(data.length | (levelIdx << 30));
    buf.writeBytes(data);
  }

  public void write39(ByteBuf buf) {
    buf.writeBoolean(fullChunk);
    buf.writeInt(x);
    buf.writeShort(y);
    buf.writeInt(z);
    buf.writeByte(xSize);
    buf.writeByte(ySize);
    buf.writeByte(zSize);
    buf.writeInt(data.length);
    buf.writeBytes(data);
  }
}