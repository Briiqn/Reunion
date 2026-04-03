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

package dev.briiqn.reunion.api;

import dev.briiqn.reunion.api.command.CommandManager;
import dev.briiqn.reunion.api.event.EventBus;
import dev.briiqn.reunion.api.messaging.PluginMessageRegistry;
import dev.briiqn.reunion.api.permission.PermissionManager;
import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.api.player.PlayerManager;
import dev.briiqn.reunion.api.plugin.PluginManager;
import dev.briiqn.reunion.api.scheduler.GlobalScheduler;
import dev.briiqn.reunion.api.world.WorldView;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The root object of the Reunion Plugin API.
 *
 * <p>Injected into your plugin via {@code ReunionPluginBase#proxy()}.
 * All major subsystems are accessible from here.
 *
 * <p>This is the equivalent of Velocity's {@code ProxyServer}.
 */
public interface ReunionProxy {

  /**
   * Returns an immutable snapshot of all currently connected players.
   */
  Collection<? extends Player> allPlayers();

  /**
   * Looks up a connected player by username.
   *
   * @param username the player's name (case-insensitive)
   * @return an Optional containing the player, or empty if offline
   */
  Optional<Player> player(String username);

  /**
   * Looks up a connected player by UUID.
   *
   * @param uuid the player's UUID
   * @return an Optional containing the player, or empty if offline
   */
  Optional<Player> player(UUID uuid);

  /**
   * Returns the total number of currently connected players.
   */
  int playerCount();

  /**
   * Returns the configured maximum number of concurrent players.
   */
  int maxPlayers();

  /**
   * The plugin manager  load, unload, and query plugins.
   */
  PluginManager pluginManager();

  /**
   * The event bus  fire and subscribe to events.
   */
  EventBus eventBus();

  /**
   * The command manager  register proxy-side commands.
   *
   * <p>Commands registered here intercept player chat messages that begin with {@code /}
   * before they are forwarded to the Java backend, as well as console input. If no registered proxy
   * command matches, the input is passed through to the backend as normal.
   */
  CommandManager commandManager();

  /**
   * The global scheduler  schedule tasks not tied to a specific plugin. Prefer using
   * {@code plugin.scheduler()} for plugin-owned tasks.
   */
  GlobalScheduler scheduler();

  /**
   * The player manager  internal player tracking and lookup.
   */
  PlayerManager playerManager();

  /**
   * The plugin messaging registry  register channel listeners and send plugin messages.
   */
  PluginMessageRegistry messagingRegistry();

  /**
   * The permission manager  check, grant, and revoke permissions for players and groups.
   *
   * <p>Backed by a local SQLite database ({@code data/permissions.db}) and supports
   * permission groups, group inheritance, wildcard nodes, and per-player overrides.
   *
   * <p>Example:
   * <pre>{@code
   * // Inside a SimpleCommand.execute():
   * if (!invocation.source().hasPermission("myplugin.admin")) {
   *     invocation.source().sendMessage("No permission.");
   *     return;
   * }
   * }</pre>
   */
  PermissionManager permissionManager();

  /**
   * Returns a read-only view of the current game state as seen by a specific player. Because
   * Reunion is a proxy (not a real server), world state is per-player.
   *
   * @param player the player whose world view to retrieve
   * @return the player's world view
   */
  WorldView worldView(Player player);

  /**
   * Returns the proxy version string, e.g. "Reunion 1.0.0".
   */
  String version();

  /**
   * Returns {@code true} if the proxy is still running.
   */
  boolean isRunning();

  /**
   * Initiates a graceful shutdown of the proxy.
   *
   * @param reason the reason sent to all connected clients
   */
  void shutdown(String reason);
}