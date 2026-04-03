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

import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.core.plugin.hooks.PermissionHooks;

/**
 * A {@link CommandSource} backed by a connected {@link Player}.
 *
 * <p>Messages sent to this source are forwarded to the player's console client
 * via {@link Player#sendMessage(String)}.
 *
 * <p>Permission checks ({@link #hasPermission(String)}) are delegated to the
 * proxy permission system via {@link PermissionHooks}. If the permission manager has not yet been
 * initialised, all checks return {@code true}.
 */
public final class PlayerCommandSource implements CommandSource {

  private final Player player;

  public PlayerCommandSource(Player player) {
    this.player = player;
  }

  @Override
  public void sendMessage(String message) {
    player.sendMessage(message);
  }

  @Override
  public String name() {
    return player.username();
  }

  @Override
  public boolean isConsole() {
    return false;
  }

  @Override
  public boolean isPlayer() {
    return true;
  }

  @Override
  public boolean hasPermission(String permission) {
    return PermissionHooks.hasPermission(player.uuid(), permission);
  }

  @Override
  public Player asPlayer() {
    return player;
  }

  @Override
  public String toString() {
    return "PlayerCommandSource{" + player.username() + "}";
  }
}