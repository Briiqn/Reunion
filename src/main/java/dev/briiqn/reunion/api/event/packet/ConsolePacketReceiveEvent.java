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
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;

/**
 * Fired when a fully decoded {@link ConsoleC2SPacket} has been received from the LCE client,
 * immediately before Reunion calls its {@code handle()} method.
 *
 * <p>Cancel this event to silently drop the packet  {@code handle()} will not be
 * called and the packet will not be forwarded to the Java backend.
 *
 * <p>The {@code packet()} accessor returns the fully populated, typed packet object.
 * Use Java pattern matching to work with specific packet types:
 *
 * <pre>{@code
 * @Subscribe
 * public void onConsolePacket(ConsolePacketReceiveEvent event) {
 *     if (event.packet() instanceof ConsoleChatC2SPacket chat) {
 *         if (chat.getMessage().contains("spam")) {
 *             event.cancel();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>The packet ID and protocol version are readable from the packet's
 * {@code @PacketInfo} annotation via {@link PacketInfo}.
 *
 * <p><b>Warning:</b> Cancelling packets the client or backend depends on
 * (keep-alives, teleport acks, transactions, etc) will break the session.
 */
public final class ConsolePacketReceiveEvent extends CancellableEvent {

  private final Player player;
  private final ConsoleC2SPacket packet;
  private final int protocolVersion;

  public ConsolePacketReceiveEvent(Player player, ConsoleC2SPacket packet, int protocolVersion) {
    this.player = player;
    this.packet = packet;
    this.protocolVersion = protocolVersion;
  }

  /**
   * The player whose console client sent this packet.
   */
  public Player player() {
    return player;
  }

  /**
   * The fully decoded, typed packet object.
   *
   * <p>Cast or use pattern matching to access packet-specific fields:
   * <pre>{@code
   * if (event.packet() instanceof ConsoleMovePlayerPosC2SPacket move) {
   *     double x = move.getX();
   * }
   * }</pre>
   */
  public ConsoleC2SPacket packet() {
    return packet;
  }

  /**
   * The console client's protocol version (e.g. {@code 39} or {@code 78}).
   */
  public int protocolVersion() {
    return protocolVersion;
  }

  /**
   * Convenience: returns the numeric packet ID from this packet's {@link PacketInfo} annotation, or
   * {@code -1} if the annotation is absent.
   */
  public int packetId() {
    PacketInfo info = packet.getClass().getAnnotation(PacketInfo.class);
    return info != null ? info.id() : -1;
  }
}
