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

package dev.briiqn.reunion.core.plugin.session;

import dev.briiqn.libdeflate.LibDeflate;
import dev.briiqn.reunion.api.packet.PacketPipeline;
import dev.briiqn.reunion.api.player.GameMode;
import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.api.player.PlayerInventory;
import dev.briiqn.reunion.api.world.BlockBuffer;
import dev.briiqn.reunion.api.world.BlockPosition;
import dev.briiqn.reunion.api.world.Dimension;
import dev.briiqn.reunion.api.world.Location;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleBlockRegionUpdateS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleCustomPayloadS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleMovePlayerPosRotS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTileUpdateS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaCustomPayloadC2SPacket;
import dev.briiqn.reunion.core.network.server.ServerSwitchHandler;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.FourJCompressUtil;
import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import dev.briiqn.reunion.core.world.SimpleBlockBuffer;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a live {@link ConsoleSession} as the plugin-facing {@link Player} interface.
 */
public final class ConsoleSessionPlayerAdapter implements Player {

  private static final Map<String, Map<String, Object>> METADATA = new ConcurrentHashMap<>();
  private final ConsoleSession session;
  private final PacketPipeline pipeline;

  public ConsoleSessionPlayerAdapter(ConsoleSession session) {
    this.session = session;
    this.pipeline = new ConsoleSessionPacketPipeline(session);
  }

  public ConsoleSession session() {
    return session;
  }


  @Override
  public String username() {
    return session.getPlayerName();
  }

  @Override
  public UUID uuid() {
    UUID stored = session.getUuid();
    return stored != null ? stored
        : UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getPlayerName()).getBytes());
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return (InetSocketAddress) session.getConsoleChannel().remoteAddress();
  }

  @Override
  public int clientVersion() {
    return session.getClientVersion();
  }


  @Override
  public boolean isOnline() {
    return session.getConsoleChannel().isActive();
  }

  @Override
  public boolean isConnectedToJava() {
    return session.getJavaSession() != null
        && session.getJavaSession().getJavaChannel() != null
        && session.getJavaSession().getJavaChannel().isActive();
  }

  @Override
  public Optional<String> currentServer() {
    String s = session.getCurrentServer();
    if (s != null) {
      return Optional.of(s);
    }
    var entry = session.getServer().getConfig().getDefaultServer();
    return entry != null ? Optional.of(entry.name()) : Optional.empty();
  }

  @Override
  public GameMode gameMode() {
    return GameMode.fromId(session.getGamemode());
  }


  @Override
  public Location location() {
    Vec3d pos = session.getLastPos();
    Vec2f rot = session.getLastRot();
    return Location.of(
        pos.x(), pos.y(), pos.z(),
        rot.yaw(), rot.pitch(),
        Dimension.fromIdOrDefault(session.getDimension())
    );
  }

  @Override
  public float health() {
    return session.getHealth();
  }

  @Override
  public int foodLevel() {
    return session.getFoodLevel();
  }

  @Override
  public boolean isSneaking() {
    return session.isSneaking();
  }

  @Override
  public boolean isSprinting() {
    return session.isSprinting();
  }

  @Override
  public boolean isRiding() {
    return session.getRidingEntityId() != -1;
  }

  @Override
  public int ridingEntityId() {
    return session.getRidingEntityId();
  }


  @Override
  public PlayerInventory inventory() {
    return new ConsoleSessionInventory(session);
  }


  @Override
  public void sendMessage(String message) {
    session.queueOrSendChat(message);
  }

  @Override
  public void sendPluginMessage(String channel, byte[] data) {
    PacketManager.sendToConsole(session, new ConsoleCustomPayloadS2CPacket(channel, data));
  }

  @Override
  public void sendPluginMessageToServer(String channel, byte[] data) {

    PacketManager.sendToJava(session.getJavaSession(),
        new JavaCustomPayloadC2SPacket(channel, Unpooled.wrappedBuffer(data)));
  }

  @Override
  public void disconnect(String reason) {
    PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
    session.getConsoleChannel().close();
  }

  @Override
  public void disconnectFromBackend() {
    session.getServer().getPluginApi().rawServerBridge().disconnectBackend(this);
  }

  @Override
  public void connectTo(String serverName) {
    var entry = session.getServer().getConfig().getServer(serverName);
    if (entry == null) {
      throw new IllegalArgumentException("Unknown server: " + serverName);
    }
    ServerSwitchHandler.switchServer(session, entry);
  }

  @Override
  public void teleport(Location location) {
    double consoleX = location.x() - session.getWorldOffsetX();
    double consoleZ = location.z() - session.getWorldOffsetZ();
    double stance = location.y() + 1.62f;
    session.setLastPos(new Vec3d(location.x(), location.y(), location.z()));
    PacketManager.sendToConsole(session, new ConsoleMovePlayerPosRotS2CPacket(
        consoleX, stance, location.y(), consoleZ,
        location.yaw(), location.pitch(), (byte) 1));
  }


  @Override
  public void sendBlockChange(BlockPosition pos, int blockId, int metadata) {
    int consoleX = session.toConsoleX(pos.x());
    int consoleZ = session.toConsoleZ(pos.z());
    PacketManager.sendToConsole(session,
        new ConsoleTileUpdateS2CPacket(consoleX, pos.y(), consoleZ, blockId, metadata));
  }

  @Override
  public void sendBlockBuffer(BlockPosition origin, BlockBuffer buffer) {
    if (!(buffer instanceof SimpleBlockBuffer sbb)) {
      throw new IllegalArgumentException("Unsupported BlockBuffer implementation");
    }

    int consoleX = session.toConsoleX(origin.x());
    int consoleZ = session.toConsoleZ(origin.z());

    byte[] flatData = sbb.getLCEData();
    byte[] rleData = FourJCompressUtil.rleEncode(flatData);
    byte[] compressedData = LibDeflate.compress(rleData, 6);

    int sectionY = origin.y() & ~15;
    PacketManager.sendToConsole(session, new ConsoleBlockRegionUpdateS2CPacket(
        false, consoleX, sectionY, consoleZ,
        sbb.getPacketWidth(), sbb.getPacketHeight(), sbb.getPacketDepth(),
        compressedData, session.getDimension()));
  }


  @Override
  public PacketPipeline packetPipeline() {
    return pipeline;
  }

  @Override
  public byte sessionSmallId() {
    return session.getSmallId();
  }

  @Override
  public int javaEntityId() {
    return session.getJavaEntityId();
  }

  @Override
  public long ticksAlive() {
    return session.getTicksAlive();
  }


  @Override
  public void setMetadata(String key, Object value) {
    METADATA.computeIfAbsent(username(), k -> new ConcurrentHashMap<>()).put(key, value);
  }

  @Override
  public Optional<Object> getMetadata(String key) {
    Map<String, Object> map = METADATA.get(username());
    return map == null ? Optional.empty() : Optional.ofNullable(map.get(key));
  }

  @Override
  public void removeMetadata(String key) {
    Map<String, Object> map = METADATA.get(username());
    if (map != null) {
      map.remove(key);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConsoleSessionPlayerAdapter other)) {
      return false;
    }
    return username().equalsIgnoreCase(other.username());
  }

  @Override
  public int hashCode() {
    return username().toLowerCase().hashCode();
  }

  @Override
  public String toString() {
    return "Player{" + username() + "}";
  }
}