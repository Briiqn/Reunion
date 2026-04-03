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
import dev.briiqn.reunion.core.network.packet.data.enums.ServerSetting;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 2, supports = {39, 78})
public final class ConsolePreLoginS2CPacket extends ConsoleS2CPacket {

  private final int clientVersion;
  private final String playerName;

  public ConsolePreLoginS2CPacket() {
    this(0, "Player");
  }

  public ConsolePreLoginS2CPacket(int clientVersion, String playerName) {
    this.clientVersion = clientVersion;
    this.playerName = playerName;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeShort(clientVersion);
    writeConsoleUtf(buf, playerName, 32);
    buf.writeByte(0); // friendsOnly
    buf.writeInt(0);  // ugcPlayersVersion
    buf.writeByte(0); // playerCount
    buf.writeZero(14); // SAVE_NAME_LEN

    int serverSettings = ServerSetting.build(
        ServerSetting.DIFFICULTY_EASY,
        ServerSetting.SHOW_GAMERTAGS,
        ServerSetting.GENERATE_STRUCTURES,
        ServerSetting.PVP_ENABLED,
        ServerSetting.TRUST_PLAYERS,
        ServerSetting.TNT_ENABLED,
        ServerSetting.SPREAD_FIRE
    );

    buf.writeInt(serverSettings);

    buf.writeByte(0);
    buf.writeInt(0);
  }
}