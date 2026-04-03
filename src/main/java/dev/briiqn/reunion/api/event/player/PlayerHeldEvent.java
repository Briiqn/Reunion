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
 * Fired when a player whose connection was held at {@link PreLoginEvent} sends their full login
 * packet  meaning the LCE client is ready to receive world data.
 *
 * <p>At this point the player has a valid username and client version but
 * <b>no Java backend session</b>. The plugin that held the player is responsible
 * for sending fake world packets and eventually calling {@code player.connectTo()}.
 *
 * <pre>{@code
 * @Subscribe
 * public void onHeld(PlayerHeldEvent event) {
 *     sendFakeWorld(event.player());
 *     scheduler().runLater(() -> event.player().connectTo("lobby"), 3, TimeUnit.SECONDS);
 * }
 * }</pre>
 */
public record PlayerHeldEvent(Player player) implements Event {

  /**
   * The player who is now in the held/limbo state. They have a console connection but no Java
   * backend session yet.
   */
  @Override
  public Player player() {
    return player;
  }
}