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

package dev.briiqn.reunion.core.network.packet.protocol.console.c2s.impl;

import static dev.briiqn.reunion.core.util.StringUtil.readConsoleUtf;

import dev.briiqn.reunion.api.event.player.PreLoginEvent;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsolePreLoginS2CPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 2, supports = {39, 78})
public final class ConsolePreLoginC2SPacket extends ConsoleC2SPacket {

  private int clientVersion;
  private String playerName;

  public ConsolePreLoginC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    clientVersion = buf.readUnsignedShort();
    playerName = readConsoleUtf(buf);

    buf.readByte(); // friendsOnlyBits
    buf.readInt();  // ugcPlayersVersion

    int playerCount = buf.readUnsignedByte();
    for (int i = 0; i < playerCount; i++) {
      buf.readLong();
    }

    buf.skipBytes(14); // szUniqueSaveName
    buf.readInt();     // serverSettings
    buf.readByte();    // hostIndex
    buf.readInt();     // texturePackId
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    session.setClientVersion(clientVersion);
    session.setPlayerName(playerName);

    InetSocketAddress address =
        (InetSocketAddress) session.getConsoleChannel().remoteAddress();

    PreLoginEvent event = PluginEventHooks.firePreLogin(
        session, playerName, address, clientVersion);

    switch (event.result().status()) {

      case DENIED -> {
        PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
        session.getConsoleChannel().close();
      }

      case HELD -> {
        //we only send the prelogin to trick the client into thinking we have established a connection to the backend server when we haven't yet.
        PacketManager.sendToConsole(session,
            new ConsolePreLoginS2CPacket(clientVersion, playerName));
        session.getConsoleChannel().config().setAutoRead(true);
      }

      default -> session.initiateJavaConnection();
    }
  }
}