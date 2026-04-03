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
import dev.briiqn.reunion.api.world.BlockPosition;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;

/**
 * Fired when a player sends a block-digging / player-action packet (start dig, abort dig, finish
 * dig, drop item, release use-item, etc.).
 *
 * <p>Cancel this event to suppress the action from being forwarded to the Java backend.
 *
 * <pre>{@code
 * @Subscribe
 * public void onAction(PlayerActionEvent event) {
 *      *     if (event.action() == 0 && isProtected(event.blockPosition())) {
 *         event.cancel();
 *     }
 * }
 * }</pre>
 */
public final class PlayerActionEvent extends CancellableEvent {

  private final Player player;
  private final int action;
  private final int x;
  private final int y;
  private final int z;
  private final int face;

  public PlayerActionEvent(Player player, int action, int x, int y, int z, int face) {
    this.player = player;
    this.action = action;
    this.x = x;
    this.y = y;
    this.z = z;
    this.face = face;
  }

  /**
   * The player performing the action.
   */
  public Player player() {
    return player;
  }

  /**
   * The raw digging action ID as used in the Java 1.8 protocol:
   * <ul>
   *   <li>0  start digging</li>
   *   <li>1  abort digging</li>
   *   <li>2  finish digging</li>
   *   <li>3  drop item stack</li>
   *   <li>4  drop item</li>
   *   <li>5  release use-item (stop eating / bow release)</li>
   * </ul>
   */
  public int action() {
    return action;
  }

  /**
   * The Java-side X coordinate of the targeted block (world space, post-offset).
   */
  public int x() {
    return x;
  }

  /**
   * The Y coordinate of the targeted block.
   */
  public int y() {
    return y;
  }

  /**
   * The Java-side Z coordinate of the targeted block (world space, post-offset).
   */
  public int z() {
    return z;
  }

  /**
   * The block face that was targeted (0–5).
   */
  public int face() {
    return face;
  }

  /**
   * The targeted block as a {@link BlockPosition}.
   */
  public BlockPosition blockPosition() {
    return new BlockPosition(x, y, z);
  }

  /**
   * The targeted block position as a JOML {@link Vec3d}.
   */
  public Vec3d position() {
    return new Vec3d(x, y, z);
  }
}