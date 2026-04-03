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
 * Fired after a player has sent the full login packet and their {@link Player} object has been
 * constructed, but before they have been connected to the backend Java server.
 *
 * <p>This is the right place to perform ban checks, whitelist enforcement, or
 * any logic that needs the full player object but should happen before the Java connection is
 * established.
 *
 * <p>Deny this event to disconnect the player with a message:
 * <pre>{@code
 * @Subscribe
 * public void onLogin(LoginEvent event) {
 *     if (banManager.isBanned(event.player().username())) {
 *         event.deny("You are banned: " + banManager.getReason(event.player().username()));
 *     }
 * }
 * }</pre>
 */
public final class LoginEvent extends ResultedEvent {

  private final Player player;

  public LoginEvent(Player player) {
    this.player = player;
  }

  /**
   * The player who is logging in. The player is not yet in the online player list when this event
   * fires.
   */
  public Player player() {
    return player;
  }
}
