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

package dev.briiqn.reunion.api.scheduler;

import dev.briiqn.reunion.api.plugin.Plugin;


/**
 * A handle to a task that has been scheduled with the proxy scheduler.
 *
 * <p>Use {@link #cancel()} to stop a repeating task before it fires again,
 * or to abort a delayed task before it executes.
 */
public interface ScheduledTask {

  /**
   * Attempts to cancel this task.
   *
   * <p>If the task is currently executing, this call has no effect on the
   * current execution but prevents future executions.
   */
  void cancel();

  /**
   * Returns {@code true} if this task has been cancelled.
   */
  boolean isCancelled();

  /**
   * Returns {@code true} if this task has finished executing (for one-shot tasks), or if it is an
   * active repeating task.
   */
  boolean isDone();

  /**
   * The plugin that owns this task.
   */
  Plugin plugin();
}
