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

package dev.briiqn.reunion.api.messaging;

import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.api.plugin.Plugin;
import java.util.Set;

/**
 * Registry for plugin messaging channels.
 *
 * <p>Reunion supports the Minecraft plugin messaging protocol ({@code 0x3F}
 * custom payload packets). Channels can originate from the console client (C2S) or from the Java
 * backend (S2C).
 *
 * <p>When a message is received on a registered channel, all registered
 * {@link PluginMessageHandler}s for that channel are invoked.
 *
 * <p>Example  listening for messages from the Java backend:
 * <pre>{@code
 * proxy().messagingRegistry().registerInbound(plugin, "my:channel", (player, data) -> {
 *     String text = new String(data, StandardCharsets.UTF_8);
 *     player.sendMessage("Server says: " + text);
 * });
 * }</pre>
 *
 * <p>Example  sending a message to the Java backend:
 * <pre>{@code
 * player.sendPluginMessageToServer("my:channel", "hello".getBytes(StandardCharsets.UTF_8));
 * }</pre>
 */
public interface PluginMessageRegistry {

  /**
   * Registers a handler for messages arriving from the Java backend on the given channel.
   *
   * <p>The handler is invoked on the Netty I/O thread  keep it fast and
   * non-blocking, or dispatch to a separate executor for heavy work.
   *
   * @param plugin  the owning plugin
   * @param channel the channel identifier (e.g. {@code "my:data"})
   * @param handler the handler to invoke
   */
  void registerInbound(Plugin plugin, String channel, PluginMessageHandler handler);

  /**
   * Registers a handler for messages arriving from the console client on the given channel.
   *
   * @param plugin  the owning plugin
   * @param channel the channel identifier
   * @param handler the handler to invoke
   */
  void registerOutbound(Plugin plugin, String channel, PluginMessageHandler handler);

  /**
   * Registers a handler for plugin messages from either direction on the given channel.
   *
   * @param plugin  the owning plugin
   * @param channel the channel identifier
   * @param handler the handler to invoke
   */
  void register(Plugin plugin, String channel, PluginMessageHandler handler);

  /**
   * Unregisters all handlers for the given channel owned by the given plugin.
   *
   * @param plugin  the owning plugin
   * @param channel the channel identifier
   */
  void unregister(Plugin plugin, String channel);

  /**
   * Unregisters all handlers owned by the given plugin across all channels. Called automatically
   * when a plugin is disabled.
   *
   * @param plugin the plugin whose handlers to remove
   */
  void unregisterAll(Plugin plugin);

  /**
   * Returns an unmodifiable set of all currently registered channel identifiers.
   */
  Set<String> registeredChannels();

  /**
   * Returns {@code true} if there is at least one handler registered for the given channel.
   *
   * @param channel the channel identifier
   */
  boolean isRegistered(String channel);

  /**
   * Broadcasts a plugin message on the given channel to all connected players' Java backend
   * sessions.
   *
   * @param channel the channel identifier
   * @param data    the message payload
   */
  void broadcastToServers(String channel, byte[] data);

  /**
   * Sends a plugin message from the proxy to a specific player's Java backend session.
   *
   * @param player  the target player
   * @param channel the channel identifier
   * @param data    the message payload
   */
  void sendToServer(Player player, String channel, byte[] data);

  /**
   * Sends a plugin message from the proxy to a specific player's console client.
   *
   * @param player  the target player
   * @param channel the channel identifier
   * @param data    the message payload
   */
  void sendToClient(Player player, String channel, byte[] data);
}
