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
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;

/**
 * Fired just before a {@link JavaC2SPacket} is written to the Java backend server's outbound
 * channel on behalf of a player.
 *
 * <p>Cancel this event to suppress the packet  the Java server will not receive it.
 *
 * <p>The {@code packet()} accessor returns the fully constructed, typed packet object.
 * Use Java pattern matching to work with specific types:
 *
 * <pre>{@code
 * @Subscribe
 * public void onJavaSend(JavaPacketSendEvent event) {
 *      *     if (event.packet() instanceof JavaChatMessageC2SPacket chat) {
 *         if (chat.getMessage().startsWith("/stop")) {
 *             event.cancel();
 *             event.player().sendMessage("That command is not allowed.");
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Warning:</b> Suppressing packets the Java backend expects (keep-alives,
 * teleport confirmations, position updates) will cause the backend server to time out or kick the
 * player.
 */
public final class JavaPacketSendEvent extends CancellableEvent {

  private final Player player;
  private final JavaC2SPacket packet;
  private final int protocolVersion;

  public JavaPacketSendEvent(Player player, JavaC2SPacket packet, int protocolVersion) {
    this.player = player;
    this.packet = packet;
    this.protocolVersion = protocolVersion;
  }

  /**
   * The player whose session is sending this packet.
   */
  public Player player() {
    return player;
  }

  /**
   * The fully constructed, typed outbound packet.
   *
   * <p>Pattern-match to inspect specific packet types:
   * <pre>{@code
   * if (event.packet() instanceof JavaPlayerPositionC2SPacket pos) {
   *          * }
   * }</pre>
   */
  public JavaC2SPacket packet() {
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
