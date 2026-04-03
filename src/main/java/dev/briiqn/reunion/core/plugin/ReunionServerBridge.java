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

package dev.briiqn.reunion.core.plugin;

import dev.briiqn.reunion.api.ReunionProxyImpl;
import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.api.world.Location;
import dev.briiqn.reunion.api.world.WorldView;
import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleCustomPayloadS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleMovePlayerPosRotS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaCustomPayloadC2SPacket;
import dev.briiqn.reunion.core.network.server.ServerSwitchHandler;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Bridges the plugin API's {@link ReunionProxyImpl.ServerBridge} to the live {@link ReunionServer}
 * and its sessions.
 */
public final class ReunionServerBridge implements ReunionProxyImpl.ServerBridge {

  private final ReunionServer server;

  public ReunionServerBridge(ReunionServer server) {
    this.server = server;
  }

  @Override
  public Collection<? extends Player> allPlayers() {
    return server.getSessions().values().stream()
        .map(ConsoleSessionPlayerAdapter::new)
        .toList();
  }

  @Override
  public Optional<Player> player(String username) {
    ConsoleSession session = server.getSession(username);
    return Optional.ofNullable(session).map(ConsoleSessionPlayerAdapter::new);
  }

  @Override
  public Optional<Player> player(UUID uuid) {
    return server.getSessions().values().stream()
        .filter(s -> uuid.equals(s.getUuid()))
        .findFirst()
        .map(ConsoleSessionPlayerAdapter::new);
  }

  @Override
  public int maxPlayers() {
    return server.getConfig().getGameplay().getMaxPlayers();
  }

  @Override
  public void disconnectPlayer(Player player, String reason) {
    ConsoleSession session = resolve(player);
    if (session == null) {
      return;
    }
    PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
    session.getConsoleChannel().close();
  }

  @Override
  public void disconnectBackend(Player player) {
    ConsoleSession session = resolve(player);
    if (session == null || session.getJavaSession() == null) {
      return;
    }

    session.getJavaSession().setSwitching(true);
    session.getJavaSession().close(true);
    session.setJavaSession(null);
    session.setCurrentServer(null);
    session.cleanupForWorldChange();
  }

  @Override
  public void connectPlayer(Player player, String serverName) {
    ConsoleSession session = resolve(player);
    if (session == null) {
      return;
    }
    var entry = server.getConfig().getServer(serverName);
    if (entry == null) {
      throw new IllegalArgumentException("Unknown server: " + serverName);
    }
    ServerSwitchHandler.switchServer(session, entry);
  }

  @Override
  public void teleportPlayer(Player player, Location location) {
    ConsoleSession session = resolve(player);
    if (session == null) {
      return;
    }
    double consoleX = location.x() - session.getWorldOffsetX();
    double consoleZ = location.z() - session.getWorldOffsetZ();
    double stance = location.y() + 1.62f;
    PacketManager.sendToConsole(session, new ConsoleMovePlayerPosRotS2CPacket(
        consoleX, stance, location.y(), consoleZ,
        location.yaw(), location.pitch(), (byte) 1));
  }

  @Override
  public void sendToConsole(Player player, String channel, byte[] data) {
    ConsoleSession session = resolve(player);
    if (session == null) {
      return;
    }
    PacketManager.sendToConsole(session, new ConsoleCustomPayloadS2CPacket(channel, data));
  }

  @Override
  public void sendToJava(Player player, String channel, byte[] data) {
    ConsoleSession session = resolve(player);
    if (session == null || session.getJavaSession() == null) {
      return;
    }
    PacketManager.sendToJava(session.getJavaSession(),
        new JavaCustomPayloadC2SPacket(channel, Unpooled.wrappedBuffer(data)));
  }

  @Override
  public WorldView worldView(Player player) {
    ConsoleSession session = resolve(player);
    if (session == null) {
      return null;
    }
    return new ConsoleSessionWorldView(session);
  }


  @Override
  public String version() {
    return "Reunion "+ReunionServer.releaseVersion;
  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public void shutdown(String reason) {
    allPlayers().forEach(p -> disconnectPlayer(p, reason));
    server.stop();
  }


  private ConsoleSession resolve(Player player) {
    if (player instanceof ConsoleSessionPlayerAdapter adapter) {
      return adapter.session();
    }
    return server.getSession(player.username());
  }
}