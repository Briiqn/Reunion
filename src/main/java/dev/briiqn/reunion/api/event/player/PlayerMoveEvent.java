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
import dev.briiqn.reunion.api.world.Location;
import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;


/**
 * Fired when a player sends a movement packet (position or position+look).
 *
 * <p>Cancel this event to suppress forwarding the movement to the Java backend.
 * The proxy-side position fields ({@code lastX/Y/Z}) are still updated so the proxy stays in sync;
 * only the C2S packet to the backend is blocked.
 *
 * <pre>{@code
 * @Subscribe
 * public void onMove(PlayerMoveEvent event) {
 *      *     Vector3d to = event.toPosition();
 *     if (to.distance(spawn) > 100) {
 *         event.cancel();
 *     }
 * }
 * }</pre>
 */
public final class PlayerMoveEvent extends CancellableEvent {

  private final Player player;
  private final Location from;
  private final Location to;

  public PlayerMoveEvent(Player player, Location from, Location to) {
    this.player = player;
    this.from = from;
    this.to = to;
  }

  /**
   * The player who is moving.
   */
  public Player player() {
    return player;
  }

  /**
   * The player's previous location.
   */
  public Location from() {
    return from;
  }

  /**
   * The player's new location.
   */
  public Location to() {
    return to;
  }

  /**
   * The player's previous (x, y, z) as a JOML {@link Vec3d}.
   */
  public Vec3d fromPosition() {
    return from.position();
  }

  /**
   * The player's new (x, y, z) as a JOML {@link Vec3d}.
   */
  public Vec3d toPosition() {
    return to.position();
  }

  /**
   * The player's new (yaw, pitch) as a JOML {@link Vec2f}.
   */
  public Vec2f toRotation() {
    return to.rotation();
  }
}