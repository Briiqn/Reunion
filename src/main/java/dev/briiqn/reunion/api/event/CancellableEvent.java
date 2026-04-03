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

package dev.briiqn.reunion.api.event;

import lombok.Getter;

/**
 * An event that can be cancelled, stopping it from proceeding.
 *
 * <p>When a handler cancels a {@code CancellableEvent}, the action that triggered
 * the event will not be performed. Subsequent event handlers with lower priority will still receive
 * the event unless {@link EventBus} is configured to skip them.
 *
 * <p>Example usage in a handler:
 * <pre>{@code
 * @Subscribe
 * public void onChat(PlayerChatEvent event) {
 *     if (event.message().contains("badword")) {
 *         event.cancel();
 *     }
 * }
 * }</pre>
 */
@Getter
public abstract class CancellableEvent implements Event {

  /**
   * Returns if this event has been cancelled.
   */
  private boolean cancelled = false;

  /**
   * Cancels this event, preventing the associated action from occurring.
   */
  public void cancel() {
    this.cancelled = true;
  }

  /**
   * Explicitly un-cancels this event. Use with care  overriding a previous handler's cancel
   * decision.
   */
  public void uncancel() {
    this.cancelled = false;
  }
}
