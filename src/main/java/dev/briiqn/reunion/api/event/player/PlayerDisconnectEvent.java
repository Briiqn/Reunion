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

package dev.briiqn.reunion.api.event.player;

import dev.briiqn.reunion.api.event.Event;
import dev.briiqn.reunion.api.player.Player;

/**
 * Fired when a player disconnects from the proxy for any reason.
 *
 * <p>The player has already been removed from the online list when this fires.
 * This event cannot be cancelled.
 */
public record PlayerDisconnectEvent(Player player, String reason) implements Event {

  /**
   * The player who disconnected.
   */
  @Override
  public Player player() {
    return player;
  }

  /**
   * The reason for the disconnection, if available. May be an empty string if no explicit reason
   * was given.
   */
  @Override
  public String reason() {
    return reason;
  }
}
