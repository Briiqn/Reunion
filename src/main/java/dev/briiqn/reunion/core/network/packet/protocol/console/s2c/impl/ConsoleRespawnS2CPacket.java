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
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 9, supports = {39, 78})
public final class ConsoleRespawnS2CPacket extends ConsoleS2CPacket {

  private final int dimension;
  private final int gamemode;
  private final String levelType;
  private final long seed;
  private final int difficulty;
  private final int safePlayerId;

  public ConsoleRespawnS2CPacket() {
    this(0, 0, "DEFAULT", 0L, 0, 0);
  }

  public ConsoleRespawnS2CPacket(int dimension, int gamemode, String levelType,
      long seed, int difficulty, int safePlayerId) {
    this.dimension = dimension;
    this.gamemode = gamemode;
    this.levelType = levelType;
    this.seed = seed;
    this.difficulty = difficulty;
    this.safePlayerId = safePlayerId;
  }

  @Override
  public void write(ByteBuf buf) {
    log.debug("[S2C Respawn] Switching to Dimension: {}, LevelType: {}", dimension, levelType);

    buf.writeByte(dimension);
    buf.writeByte(gamemode & 0x3);
    buf.writeShort(256);
    writeConsoleUtf(buf, levelType, 16);
    buf.writeLong(seed);
    buf.writeByte(difficulty);
    buf.writeBoolean(false);
    buf.writeShort(safePlayerId);
    buf.writeShort(512);
    buf.writeByte(8);
  }
}
