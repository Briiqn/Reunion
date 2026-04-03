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

/**
 * The source that executed a command.
 *
 * <p>A CommandSource can be the proxy console, a connected player, or any
 * custom source registered by a plugin.
 */
public interface CommandSource {

  /**
   * Sends a message back to this source.
   *
   * @param message the message text (§ formatting codes accepted)
   */
  void sendMessage(String message);

  /**
   * Returns the name of this source (e.g. the player's username or "CONSOLE").
   */
  String name();

  /**
   * Returns {@code true} if this source is the proxy console.
   */
  boolean isConsole();

  /**
   * Returns {@code true} if this source is a connected player.
   */
  boolean isPlayer();

  /**
   * Returns {@code true} if this source holds the given permission node.
   *
   * <p>The console always returns {@code true}. For players, the result is
   * determined by the proxy permission system (see {@code proxy().permissionManager()}).
   *
   * <p>Wildcard nodes are supported (e.g. {@code "myplugin.*"} grants every
   * node under that namespace). Use this inside {@link SimpleCommand#hasPermission} or directly
   * inside {@link SimpleCommand#execute} for finer-grained checks:
   * <pre>{@code
   * if (!invocation.source().hasPermission("myplugin.admin")) {
   *     invocation.source().sendMessage("No permission.");
   *     return;
   * }
   * }</pre>
   *
   * @param permission the permission node to test (e.g. {@code "myplugin.command.reload"})
   * @return {@code true} if this source holds the permission
   */
  default boolean hasPermission(String permission) {
    return true;
  }

  /**
   * Casts this source to a {@link dev.briiqn.reunion.api.player.Player}.
   *
   * @return the player
   * @throws UnsupportedOperationException if this source is not a player
   */
  default dev.briiqn.reunion.api.player.Player asPlayer() {
    throw new UnsupportedOperationException("This CommandSource is not a Player: " + name());
  }
}