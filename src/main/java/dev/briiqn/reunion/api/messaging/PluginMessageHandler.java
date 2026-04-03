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

/**
 * Handles an incoming plugin message on a registered channel.
 *
 * <p>Implementations should be fast. If you need to do heavy processing,
 * hand the data off to a separate thread rather than blocking the I/O thread.
 *
 * <p>Example:
 * <pre>{@code
 * registry.registerInbound(plugin, "my:sync", (player, data) -> {
 *     executor.submit(() -> processSyncData(player, data));
 * });
 * }</pre>
 */
@FunctionalInterface
public interface PluginMessageHandler {

  /**
   * Called when a plugin message is received on the registered channel.
   *
   * @param player the player whose connection carried this message
   * @param data   the raw message payload bytes (do not mutate)
   */
  void handle(Player player, byte[] data);
}
