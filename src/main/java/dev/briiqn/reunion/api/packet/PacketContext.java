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

package dev.briiqn.reunion.api.packet;

import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;

/**
 * Provides context about a packet that is currently in flight through the pipeline.
 *
 * <p>This is the object passed to every registered {@link PacketInterceptor}.
 * The {@code packet()} method returns the fully decoded, typed packet object  use pattern matching
 * to inspect specific types.
 *
 * <p>The four directional helpers ({@link #asConsoleInbound()}, etc.) cast the
 * packet to its specific base type without requiring an explicit cast in calling code.
 */
public interface PacketContext {

  /**
   * The player whose connection this packet belongs to.
   */
  Player player();

  /**
   * The direction this packet is travelling through the pipeline.
   */
  PacketDirection direction();

  /**
   * The fully decoded packet object.
   *
   * <p>Use pattern matching to access specific fields:
   * <pre>{@code
   * PacketContext ctx = ...;
   * if (ctx.packet() instanceof ConsoleMovePlayerPosC2SPacket move) {
   *     double x = move.getX();
   * }
   * }</pre>
   */
  Packet packet();

  /**
   * The protocol version for this connection. Console: 39 or 78. Java: 47.
   */
  int protocolVersion();

  /**
   * Returns the packet cast as {@link ConsoleC2SPacket}, or {@code null} if this packet is not
   * travelling {@link PacketDirection#CONSOLE_TO_PROXY}.
   */
  default ConsoleC2SPacket asConsoleInbound() {
    return (packet() instanceof ConsoleC2SPacket p) ? p : null;
  }

  /**
   * Returns the packet cast as {@link JavaS2CPacket}, or {@code null} if this packet is not
   * travelling {@link PacketDirection#JAVA_TO_PROXY}.
   */
  default JavaS2CPacket asJavaInbound() {
    return (packet() instanceof JavaS2CPacket p) ? p : null;
  }

  /**
   * Returns the packet cast as {@link ConsoleS2CPacket}, or {@code null} if this packet is not
   * travelling {@link PacketDirection#PROXY_TO_CONSOLE}.
   */
  default ConsoleS2CPacket asConsoleOutbound() {
    return (packet() instanceof ConsoleS2CPacket p) ? p : null;
  }

  /**
   * Returns the packet cast as {@link JavaC2SPacket}, or {@code null} if this packet is not
   * travelling {@link PacketDirection#PROXY_TO_JAVA}.
   */
  default JavaC2SPacket asJavaOutbound() {
    return (packet() instanceof JavaC2SPacket p) ? p : null;
  }

  /**
   * Convenience: returns the numeric packet ID from the packet's {@link PacketInfo} annotation, or
   * {@code -1} if absent.
   */
  default int packetId() {
    PacketInfo info = packet().getClass().getAnnotation(PacketInfo.class);
    return info != null ? info.id() : -1;
  }
}

