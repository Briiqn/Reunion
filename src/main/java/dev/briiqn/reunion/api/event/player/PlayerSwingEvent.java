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
 * Fired when a player swings their arm (sends an Animate / swing packet).
 *
 * <p>Cancel this event to suppress the swing animation from being forwarded to the
 * Java backend.
 *
 * <pre>{@code
 * @Subscribe
 * public void onSwing(PlayerSwingEvent event) {
 *     log.info("{} swung their arm", event.player().username());
 * }
 * }</pre>
 */
public final class PlayerSwingEvent extends CancellableEvent {

  private final Player player;

  public PlayerSwingEvent(Player player) {
    this.player = player;
  }

  /**
   * The player who swung.
   */
  public Player player() {
    return player;
  }
}