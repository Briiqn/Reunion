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

import java.util.concurrent.TimeUnit;

/**
 * A scheduler scoped to a specific plugin.
 *
 * <p>All tasks registered here are automatically cancelled when the plugin is disabled.
 * Obtain via {@code plugin.scheduler()} inside your
 * {@link dev.briiqn.reunion.api.plugin.ReunionPluginBase}.
 *
 * <p>Tasks run on a shared virtual-thread executor unless otherwise specified.
 *
 * <p>Example:
 * <pre>{@code
 * scheduler().runLater(() -> broadcast("Hello!"), 5, TimeUnit.SECONDS);
 *
 * ScheduledTask task = scheduler().runRepeating(() -> checkPlayers(), 1, 10, TimeUnit.SECONDS);
 *  * task.cancel();
 * }</pre>
 */
public interface PluginScheduler {

  /**
   * Runs a task asynchronously as soon as possible on a virtual thread.
   *
   * @param task the runnable to execute
   * @return a handle to the scheduled task
   */
  ScheduledTask run(Runnable task);

  /**
   * Runs a task asynchronously after the specified delay.
   *
   * @param task  the runnable to execute
   * @param delay the delay before execution
   * @param unit  the time unit of the delay
   * @return a handle to the scheduled task
   */
  ScheduledTask runLater(Runnable task, long delay, TimeUnit unit);

  /**
   * Runs a task asynchronously at a fixed rate after an initial delay.
   *
   * <p>If an execution takes longer than the period, the next execution is
   * deferred rather than skipped (fixed-delay semantics).
   *
   * @param task         the runnable to execute
   * @param initialDelay delay before the first execution
   * @param period       delay between subsequent executions
   * @param unit         the time unit for both delays
   * @return a handle to the repeating task
   */
  ScheduledTask runRepeating(Runnable task, long initialDelay, long period, TimeUnit unit);

  /**
   * Convenience overload: runs immediately then repeats every {@code period} units.
   *
   * @param task   the runnable to execute
   * @param period the period between executions
   * @param unit   the time unit of the period
   * @return a handle to the repeating task
   */
  default ScheduledTask runRepeating(Runnable task, long period, TimeUnit unit) {
    return runRepeating(task, 0, period, unit);
  }

  /**
   * Cancels all tasks owned by this plugin's scheduler. Called automatically when the plugin is
   * disabled.
   */
  void cancelAll();
}
