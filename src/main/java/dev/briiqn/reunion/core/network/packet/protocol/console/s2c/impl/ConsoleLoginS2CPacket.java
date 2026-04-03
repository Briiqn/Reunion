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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 1, supports = {39, 78})
public final class ConsoleLoginS2CPacket extends ConsoleS2CPacket {

  private final int entityId;
  private final String playerName;
  private final String levelType;
  private final long seed;
  private final int gamemode;
  private final int dimension;
  private final int difficulty;
  private final byte smallId;
  private final int uiGamePrivileges;

  public ConsoleLoginS2CPacket() {
    this(0, "", "DEFAULT", 0L, 0, 0, 0, (byte) 1, 0);
  }

  public ConsoleLoginS2CPacket(int entityId, String playerName, String levelType,
      long seed, int gamemode, int dimension, int difficulty, byte smallId,
      int uiGamePrivileges) {
    this.entityId = entityId;
    this.playerName = playerName;
    this.levelType = levelType;
    this.seed = seed;
    this.gamemode = gamemode;
    this.dimension = dimension;
    this.difficulty = difficulty;
    this.smallId = smallId;
    this.uiGamePrivileges = uiGamePrivileges;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(entityId);
    writeConsoleUtf(buf, playerName, 16);
    writeConsoleUtf(buf, levelType, 16);
    buf.writeLong(seed);
    buf.writeInt(gamemode & 0x3);
    buf.writeByte(dimension);
    buf.writeByte(128);
    buf.writeByte(8);
    buf.writeLong(0L);
    buf.writeLong(0L);
    buf.writeBoolean(false);
    buf.writeInt(0);
    buf.writeByte(difficulty);
    buf.writeInt(0);
    buf.writeByte(smallId & 0xFF);
    buf.writeInt(0);
    buf.writeInt(0);
    buf.writeBoolean(false);
    buf.writeBoolean(true);
    buf.writeInt(uiGamePrivileges);
    buf.writeShort(512);
    buf.writeByte(8);
  }
}