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

/**
 * Defines the order in which event handlers are invoked.
 *
 * <p>Handlers are called from lowest to highest priority.
 * {@link #MONITOR} is reserved for observation only  handlers at this priority should never modify
 * the event outcome.
 */
public enum EventPriority {

  /**
   * Called first. Useful for logic that should run before everything else and that may set or
   * inspect an initial state.
   */
  LOWEST(-2),

  /**
   * Called early, but after LOWEST.
   */
  LOW(-1),

  /**
   * The default priority for most event handlers.
   */
  NORMAL(0),

  /**
   * Called after NORMAL. Use for logic that should override defaults.
   */
  HIGH(1),

  /**
   * Called just before MONITOR. Use for final decision-making logic.
   */
  HIGHEST(2),

  /**
   * Called last. Intended for read-only observation (logging, metrics). Do NOT modify event state
   * at this priority.
   */
  MONITOR(3);

  private final int order;

  EventPriority(int order) {
    this.order = order;
  }

  /**
   * Returns the numeric order value (lower = earlier).
   */
  public int order() {
    return order;
  }
}
