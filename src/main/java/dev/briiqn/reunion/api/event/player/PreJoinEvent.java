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

import dev.briiqn.reunion.api.event.ResultedEvent;
import dev.briiqn.reunion.api.player.Player;

/**
 * Fired after the backend Java connection has been established, but before the player is sent their
 * initial world/login data and before they appear to other players.
 *
 * <p>This event is the last opportunity to deny the connection during the join flow.
 * After this event is allowed, the player will be considered fully joined.
 *
 * <p>Use this event to:
 * <ul>
 *   <li>Set initial player state (e.g. send resource packs)</li>
 *   <li>Perform last-chance permission checks</li>
 *   <li>Reject players if the server is full or whitelist is active</li>
 * </ul>
 *
 * <pre>{@code
 * @Subscribe
 * public void onPreJoin(PreJoinEvent event) {
 *     if (!whitelist.contains(event.player().username())) {
 *         event.deny("You are not whitelisted.");
 *     }
 * }
 * }</pre>
 */
public final class PreJoinEvent extends ResultedEvent {

  private final Player player;

  public PreJoinEvent(Player player) {
    this.player = player;
  }

  /**
   * The player about to join.
   */
  public Player player() {
    return player;
  }
}
