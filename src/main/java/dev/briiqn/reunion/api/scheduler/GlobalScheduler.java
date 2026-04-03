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
import java.util.concurrent.TimeUnit;

/**
 * Global scheduler for tasks not tied to a specific plugin.
 *
 * <p>Also serves as the backing implementation for per-plugin schedulers.
 * Prefer using {@code plugin.scheduler()} so tasks are auto-cancelled on unload.
 */
public interface GlobalScheduler {

  /**
   * Runs a task asynchronously on a virtual thread, associated with the given plugin.
   */
  ScheduledTask run(Plugin plugin, Runnable task);

  /**
   * Runs a task after a delay, associated with the given plugin.
   */
  ScheduledTask runLater(Plugin plugin, Runnable task, long delay, TimeUnit unit);

  /**
   * Runs a task at a fixed rate, associated with the given plugin.
   */
  ScheduledTask runRepeating(Plugin plugin, Runnable task, long initialDelay, long period,
      TimeUnit unit);

  /**
   * Cancels all tasks associated with the given plugin.
   */
  void cancelAll(Plugin plugin);

  /**
   * Shuts down the scheduler, cancelling all pending tasks.
   */
  void shutdown();

  /**
   * Returns a plugin-scoped view of this scheduler.
   *
   * @param plugin the owning plugin
   * @return a {@link PluginScheduler} that wraps this global scheduler
   */
  default PluginScheduler forPlugin(Plugin plugin) {
    return new PluginSchedulerImpl(this, plugin);
  }
}
