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
 * Fired when a player sends a chat message, before it is forwarded to the Java server.
 *
 * <p>Cancel this event to block the message. Modify {@link #setMessage(String)} to alter content.
 *
 * <pre>{@code
 * @Subscribe
 * public void onChat(PlayerChatEvent event) {
 *     event.setMessage(event.message().replace("badword", "****"));
 * }
 * }</pre>
 */
public final class PlayerChatEvent extends CancellableEvent {

  private final Player player;
  private String message;

  public PlayerChatEvent(Player player, String message) {
    this.player = player;
    this.message = message;
  }

  /**
   * The player who sent the message.
   */
  public Player player() {
    return player;
  }

  /**
   * The current message content (may have been modified by earlier handlers).
   */
  public String message() {
    return message;
  }

  /**
   * Replaces the message content.
   */
  public void setMessage(String message) {
    this.message = message;
  }
}
