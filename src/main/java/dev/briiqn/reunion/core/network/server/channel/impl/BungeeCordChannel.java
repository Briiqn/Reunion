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

package dev.briiqn.reunion.core.network.server.channel.impl;

import dev.briiqn.reunion.core.config.ServerEntry;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaCustomPayloadC2SPacket;
import dev.briiqn.reunion.core.network.server.ServerSwitchHandler;
import dev.briiqn.reunion.core.network.server.channel.PluginChannel;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class BungeeCordChannel extends PluginChannel {

  private final Map<String, SubChannelHandler> subChannels = new HashMap<>();

  public BungeeCordChannel() {
    super("BungeeCord", "bungeecord:main");
    subChannels.put("Connect", this::handleConnect);
    subChannels.put("ConnectOther", this::handleConnectOther);
    subChannels.put("GetServer", this::handleGetServer);
    subChannels.put("GetServers", this::handleGetServers);
    subChannels.put("PlayerCount", this::handlePlayerCount);
    subChannels.put("PlayerList", this::handlePlayerList);
    subChannels.put("Message", this::handleMessage);
    subChannels.put("KickPlayer", this::handleKickPlayer);
    subChannels.put("IP", this::handleIP);
    subChannels.put("UUID", this::handleUUID);
    subChannels.put("UUIDOther", this::handleUUIDOther);
  }

  @Override
  public void handleIncoming(ConsoleSession session, String channel, byte[] data) {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
      String sub = in.readUTF();
      SubChannelHandler handler = subChannels.get(sub);
      if (handler != null) {
        handler.handle(session, channel, in);
      }
    } catch (Exception e) {
      log.warn("[BungeeCord] Failed to parse plugin message on channel '{}': {}", channel,
          e.getMessage());
    }
  }

  private void handleConnect(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    String serverName = in.readUTF();
    if (serverName.equalsIgnoreCase(resolveCurrentServer(session))) {
      return;
    }
    ServerEntry entry = session.getServer().getConfig().getServer(serverName);
    if (entry == null) {
      return;
    }
    ServerSwitchHandler.switchServer(session, entry);
  }

  private void handleConnectOther(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    String playerName = in.readUTF();
    String serverName = in.readUTF();
    ConsoleSession target = session.getServer().getSession(playerName);
    if (target == null || serverName.equalsIgnoreCase(resolveCurrentServer(target))) {
      return;
    }
    ServerEntry entry = session.getServer().getConfig().getServer(serverName);
    if (entry == null) {
      return;
    }
    ServerSwitchHandler.switchServer(target, entry);
  }

  private void handleGetServer(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeUTF("GetServer");
    out.writeUTF(resolveCurrentServer(session));
    sendResponse(session, channel, baos.toByteArray());
  }

  private void handleGetServers(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    StringJoiner sj = new StringJoiner(", ");
    session.getServer().getConfig().getNetwork().getServers().keySet().forEach(sj::add);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeUTF("GetServers");
    out.writeUTF(sj.toString());
    sendResponse(session, channel, baos.toByteArray());
  }

  private void handlePlayerCount(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    String serverName = in.readUTF();
    Collection<ConsoleSession> sessions = session.getServer().getSessions().values();
    int count = "ALL".equalsIgnoreCase(serverName) ? sessions.size()
        : (int) sessions.stream().filter(s -> serverName.equalsIgnoreCase(resolveCurrentServer(s)))
                .count();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeUTF("PlayerCount");
    out.writeUTF(serverName);
    out.writeInt(count);
    sendResponse(session, channel, baos.toByteArray());
  }

  private void handlePlayerList(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    String serverName = in.readUTF();
    StringJoiner sj = new StringJoiner(", ");
    for (ConsoleSession s : session.getServer().getSessions().values()) {
      if ("ALL".equalsIgnoreCase(serverName) || serverName.equalsIgnoreCase(
          resolveCurrentServer(s))) {
        sj.add(s.getPlayerName());
      }
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeUTF("PlayerList");
    out.writeUTF(serverName);
    out.writeUTF(sj.toString());
    sendResponse(session, channel, baos.toByteArray());
  }

  private void handleMessage(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    String target = in.readUTF();
    String message = in.readUTF();
    if ("ALL".equalsIgnoreCase(target)) {
      session.getServer().getSessions().values().forEach(s -> sendChat(s, message));
    } else {
      ConsoleSession t = session.getServer().getSession(target);
      if (t != null) {
        sendChat(t, message);
      }
    }
  }

  private void handleKickPlayer(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    String playerName = in.readUTF();
    in.readUTF();
    ConsoleSession target = session.getServer().getSession(playerName);
    if (target != null) {
      target.getConsoleChannel().close();
    }
  }

  private void handleIP(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    InetSocketAddress addr = (InetSocketAddress) session.getConsoleChannel().remoteAddress();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeUTF("IP");
    out.writeUTF(addr.getAddress().getHostAddress());
    out.writeInt(addr.getPort());
    sendResponse(session, channel, baos.toByteArray());
  }

  private void handleUUID(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeUTF("UUID");
    out.writeUTF(resolveUuid(session).toString());
    sendResponse(session, channel, baos.toByteArray());
  }

  private void handleUUIDOther(ConsoleSession session, String channel, DataInputStream in)
      throws IOException {
    String playerName = in.readUTF();
    ConsoleSession target = session.getServer().getSession(playerName);
    if (target == null) {
      return;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    out.writeUTF("UUIDOther");
    out.writeUTF(playerName);
    out.writeUTF(resolveUuid(target).toString());
    sendResponse(session, channel, baos.toByteArray());
  }

  private void sendResponse(ConsoleSession session, String channel, byte[] data) {
    if (session.getJavaSession() != null) {
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaCustomPayloadC2SPacket(channel, Unpooled.wrappedBuffer(data)));
    }
  }

  private void sendChat(ConsoleSession session, String message) {
    session.queueOrSendChat(message);
  }

  private String resolveCurrentServer(ConsoleSession session) {
    String s = session.getCurrentServer();
    if (s != null) {
      return s;
    }
    ServerEntry def = session.getServer().getConfig().getDefaultServer();
    return def != null ? def.name() : "unknown";
  }

  private UUID resolveUuid(ConsoleSession session) {
    UUID stored = session.getUuid();
    return stored != null ? stored
        : UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + session.getPlayerName()).getBytes(StandardCharsets.UTF_8));
  }

  @FunctionalInterface
  private interface SubChannelHandler {

    void handle(ConsoleSession session, String channel, DataInputStream in) throws Exception;
  }
}