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

package dev.briiqn.reunion.api.command;

import dev.briiqn.reunion.api.plugin.Plugin;
import java.util.List;
import java.util.Set;

/**
 * Manages proxy-side command registration and dispatch.
 *
 * <p>Commands registered here are available from the proxy console and can be
 * dispatched programmatically. Players do not automatically have access to proxy commands unless
 * your plugin explicitly relays their chat messages here.
 *
 * <p>All registered commands are automatically unregistered when the owning plugin
 * is disabled.
 *
 * <p>Example:
 * <pre>{@code
 * @Override
 * public void onEnable() {
 *     CommandMeta meta = CommandMeta.builder("alert")
 *             .aliases("broadcast")
 *             .description("Broadcasts a message to all players.")
 *             .build();
 *     proxy().commandManager().register(this, meta, new AlertCommand(proxy()));
 * }
 * }</pre>
 */
public interface CommandManager {

  /**
   * Registers a command with the given metadata and handler, owned by the given plugin.
   *
   * @param plugin  the owning plugin
   * @param meta    the command metadata (name, aliases, description)
   * @param command the command handler
   * @throws IllegalArgumentException if any alias is already taken
   */
  void register(Plugin plugin, CommandMeta meta, SimpleCommand command);

  /**
   * Convenience method: registers a command with a single name and no aliases.
   *
   * @param plugin  the owning plugin
   * @param name    the command name
   * @param command the command handler
   */
  default void register(Plugin plugin, String name, SimpleCommand command) {
    register(plugin, CommandMeta.of(name), command);
  }

  /**
   * Unregisters the command with the given primary name.
   *
   * @param name the command's primary name
   */
  void unregister(String name);

  /**
   * Unregisters all commands owned by the given plugin. Called automatically when the plugin is
   * disabled.
   *
   * @param plugin the plugin whose commands to remove
   */
  void unregisterAll(Plugin plugin);

  /**
   * Returns {@code true} if a command with the given name or alias is registered.
   *
   * @param name the primary name or any alias
   */
  boolean hasCommand(String name);

  /**
   * Returns the set of all registered primary command names.
   */
  Set<String> registeredCommands();

  /**
   * Dispatches a raw command string from the given source.
   *
   * <p>The string should NOT include the leading {@code /} slash.
   * If no matching command is found, returns {@code false}.
   *
   * @param source  the command source
   * @param command the raw command string (e.g. {@code "alert Hello world"})
   * @return {@code true} if a matching command was found and executed
   */
  boolean dispatch(CommandSource source, String command);

  /**
   * Returns tab-completion suggestions for the given partial input.
   *
   * @param source  the command source
   * @param partial the partial command input (without leading slash)
   * @return a list of completion suggestions
   */
  List<String> suggest(CommandSource source, String partial);
}
