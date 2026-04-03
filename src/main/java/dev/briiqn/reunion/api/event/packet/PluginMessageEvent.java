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

/**
 * Fired when a plugin message packet ({@code MC|...} or custom channel) is received from either the
 * console client or the Java backend.
 *
 * <p>Cancel this event to prevent the message from being forwarded or dispatched
 * to registered {@link dev.briiqn.reunion.api.messaging.PluginMessageHandler} instances.
 *
 * <pre>{@code
 * @Subscribe
 * public void onPluginMessage(PluginMessageEvent event) {
 *     if ("my:channel".equals(event.channel())) {
 *         event.cancel();  *         handleMyMessage(event.player(), event.data());
 *     }
 * }
 * }</pre>
 */
public final class PluginMessageEvent extends CancellableEvent {

  private final Player player;
  private final String channel;
  private final byte[] data;
  private final Source source;

  public PluginMessageEvent(Player player, String channel, byte[] data, Source source) {
    this.player = player;
    this.channel = channel;
    this.data = data;
    this.source = source;
  }

  /**
   * The player associated with this plugin message.
   */
  public Player player() {
    return player;
  }

  /**
   * The channel identifier (e.g. {@code "MC|TrList"}, {@code "my:channel"}).
   */
  public String channel() {
    return channel;
  }

  /**
   * The raw payload bytes. Returns a direct reference  do not mutate.
   */
  public byte[] data() {
    return data;
  }

  /**
   * Whether the message came from the console client or the Java backend.
   */
  public Source source() {
    return source;
  }

  /**
   * Where the plugin message originated.
   */
  public enum Source {
    /**
     * Message was sent from the console client (C2S).
     */
    CONSOLE_CLIENT,
    /**
     * Message was sent from the Java backend server (S2C).
     */
    JAVA_SERVER
  }
}
