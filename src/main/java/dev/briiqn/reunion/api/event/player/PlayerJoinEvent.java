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
 * Fired after the player has fully joined the proxy and their session is active.
 *
 * <p>The player is now present in the online player list and can receive packets.
 * This event cannot be cancelled  to prevent joining, handle {@link PreJoinEvent} instead.
 */
public record PlayerJoinEvent(Player player) implements Event {

  /**
   * The player who joined.
   */
  @Override
  public Player player() {
    return player;
  }
}
