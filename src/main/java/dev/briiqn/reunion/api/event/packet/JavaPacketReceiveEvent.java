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

package dev.briiqn.reunion.api.event.packet;

import dev.briiqn.reunion.api.event.CancellableEvent;
import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;

/**
 * Fired when a fully decoded {@link JavaS2CPacket} has been received from the Java backend server,
 * immediately before Reunion calls its {@code handle()} method.
 *
 * <p>Cancel this event to silently drop the packet  {@code handle()} will not be
 * called and the packet will not be translated/forwarded to the console client.
 *
 * <p>Use Java pattern matching to work with specific packet types:
 *
 * <pre>{@code
 * @Subscribe
 * public void onJavaPacket(JavaPacketReceiveEvent event) {
 *     if (event.packet() instanceof JavaChatMessageS2CPacket chat) {
 *         String json = chat.getJsonMessage();
 *          *     }
 * }
 * }</pre>
 *
 * <p><b>Warning:</b> Cancelling packets the client or session logic depends on
 * (e.g. keep-alives, position/look, login) will break or desync the session.
 */
public final class JavaPacketReceiveEvent extends CancellableEvent {

  private final Player player;
  private final JavaS2CPacket packet;
  private final int protocolVersion;

  public JavaPacketReceiveEvent(Player player, JavaS2CPacket packet, int protocolVersion) {
    this.player = player;
    this.packet = packet;
    this.protocolVersion = protocolVersion;
  }

  /**
   * The player whose Java backend session received this packet.
   */
  public Player player() {
    return player;
  }

  /**
   * The fully decoded, typed packet object.
   *
   * <p>Cast or pattern-match to access specific fields:
   * <pre>{@code
   * if (event.packet() instanceof JavaSpawnMobS2CPacket spawn) {
   *     int mobType = spawn.getType();
   * }
   * }</pre>
   */
  public JavaS2CPacket packet() {
    return packet;
  }

  /**
   * The Java protocol version (always {@code 47} for 1.8).
   */
  public int protocolVersion() {
    return protocolVersion;
  }

  /**
   * Convenience: returns the numeric packet ID from this packet's {@link PacketInfo} annotation, or
   * {@code -1} if absent.
   */
  public int packetId() {
    PacketInfo info = packet.getClass().getAnnotation(PacketInfo.class);
    return info != null ? info.id() : -1;
  }
}
