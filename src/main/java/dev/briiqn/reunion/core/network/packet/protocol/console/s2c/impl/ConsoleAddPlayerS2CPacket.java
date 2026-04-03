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

import static dev.briiqn.reunion.core.util.StringUtil.writeConsoleUtf;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.GamePrivilege;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 20, supports = {39, 78})
public final class ConsoleAddPlayerS2CPacket extends ConsoleS2CPacket {

  private final int entityId;
  private final String name;
  private final int x, y, z;
  private final byte yaw, pitch, headRot;
  private final short currentItem;
  private final byte metaFlags;
  private final long xuid;
  private final long onlineXuid;
  private final int skinId;
  private final int capeId;

  public ConsoleAddPlayerS2CPacket() {
    this(0, "Player", 0, 0, 0, (byte) 0, (byte) 0, (byte) 0, (short) 0, (byte) 0, 0L, 0L, 0, 0);
  }

  public ConsoleAddPlayerS2CPacket(int entityId, String name, int x, int y, int z,
      byte yaw, byte pitch, byte headRot, short currentItem,
      byte metaFlags, long xuid, long onlineXuid,
      int skinId, int capeId) {
    this.entityId = entityId;
    this.name = name;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.headRot = headRot;
    this.currentItem = currentItem;
    this.metaFlags = metaFlags;
    this.xuid = xuid;
    this.onlineXuid = onlineXuid;
    this.skinId = skinId;
    this.capeId = capeId;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(entityId);
    writeConsoleUtf(buf, name, 16);
    buf.writeInt(x);
    buf.writeInt(y);
    buf.writeInt(z);
    buf.writeByte(yaw);
    buf.writeByte(pitch);
    buf.writeByte(headRot);
    buf.writeShort(currentItem);
    buf.writeLong(xuid);
    buf.writeLong(onlineXuid);
    buf.writeByte(0);
    buf.writeInt(skinId);
    buf.writeInt(capeId);
    buf.writeInt(GamePrivilege.grantAll());
    buf.writeByte(0);
    buf.writeByte(metaFlags);
    buf.writeByte(127);
  }
}