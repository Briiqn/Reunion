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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;

/**
 * Fired just before a {@link ConsoleS2CPacket} is written to the console client's outbound
 * channel.
 *
 * <p>Cancel this event to suppress the packet  the client will not receive it.
 *
 * <p>The {@code packet()} accessor returns the fully constructed, typed packet object.
 * You can read its fields directly since all S2C packets are fully built before sending:
 *
 * <pre>{@code
 * @Subscribe
 * public void onPacketSend(PacketSendEvent event) {
 *     if (event.packet() instanceof ConsoleChatS2CPacket chat) {
 *          *         if (chat.getMessage().contains("[Admin]") && !isAdmin(event.player())) {
 *             event.cancel();  *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Warning:</b> Suppressing packets the client expects (keep-alives,
 * position updates, chunk data) will break or desync the client session.
 */
public final class PacketSendEvent extends CancellableEvent {

  private final Player player;
  private final ConsoleS2CPacket packet;
  private final int protocolVersion;

  public PacketSendEvent(Player player, ConsoleS2CPacket packet, int protocolVersion) {
    this.player = player;
    this.packet = packet;
    this.protocolVersion = protocolVersion;
  }

  /**
   * The player this packet is being sent to.
   */
  public Player player() {
    return player;
  }

  /**
   * The fully constructed, typed outbound packet.
   *
   * <p>Pattern-match to inspect specific packet types:
   * <pre>{@code
   * if (event.packet() instanceof ConsoleSetHealthS2CPacket health) {
   *          * }
   * }</pre>
   */
  public ConsoleS2CPacket packet() {
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
   * {@code -1} if absent.
   */
  public int packetId() {
    PacketInfo info = packet.getClass().getAnnotation(PacketInfo.class);
    return info != null ? info.id() : -1;
  }
}
