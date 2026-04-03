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

import dev.briiqn.reunion.api.event.CancellableEvent;
import dev.briiqn.reunion.api.player.Player;

/**
 * Fired when a player is about to be transferred to a different backend server.
 *
 * <p>Cancel this event to prevent the server switch from occurring.
 */
public final class ServerSwitchEvent extends CancellableEvent {

  private final Player player;
  private final String previousServer;
  private final String targetServer;

  public ServerSwitchEvent(Player player, String previousServer, String targetServer) {
    this.player = player;
    this.previousServer = previousServer;
    this.targetServer = targetServer;
  }

  /**
   * The player switching servers.
   */
  public Player player() {
    return player;
  }

  /**
   * The name of the server the player is leaving. Returns an empty string on the initial
   * connection.
   */
  public String previousServer() {
    return previousServer;
  }

  /**
   * The name of the server the player is being connected to.
   */
  public String targetServer() {
    return targetServer;
  }
}
