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
 * Fired once per game tick for each online player.
 *
 * <p>This event is fired inside {@code ConsoleSession.flushTickActions()}  i.e.,
 * on the movement tick  so it is aligned with player position updates.
 *
 * <p>This event is <b>not cancellable</b>. Use it for per-tick logic such as
 * tracking, timers, or passive checks.
 *
 * <pre>{@code
 * @Subscribe
 * public void onPlayerTick(PlayerTickEvent event) {
 *     if (event.ticksAlive() % 20 == 0) {
 *         event.player().sendMessage("You've been alive for " + (event.ticksAlive() / 20) + "s");
 *     }
 * }
 * }</pre>
 */
public record PlayerTickEvent(Player player, long ticksAlive) implements Event {

  /**
   * The player being ticked.
   */
  @Override
  public Player player() {
    return player;
  }

  /**
   * The number of ticks this player has been alive since joining.
   */
  @Override
  public long ticksAlive() {
    return ticksAlive;
  }
}