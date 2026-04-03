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
 * Plugin-scoped wrapper around {@link GlobalScheduler}. All tasks are associated with the owning
 * plugin and auto-cancelled on disable.
 */
final class PluginSchedulerImpl implements PluginScheduler {

  private final GlobalScheduler global;
  private final Plugin plugin;

  PluginSchedulerImpl(GlobalScheduler global, Plugin plugin) {
    this.global = global;
    this.plugin = plugin;
  }

  @Override
  public ScheduledTask run(Runnable task) {
    return global.run(plugin, task);
  }

  @Override
  public ScheduledTask runLater(Runnable task, long delay, TimeUnit unit) {
    return global.runLater(plugin, task, delay, unit);
  }

  @Override
  public ScheduledTask runRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
    return global.runRepeating(plugin, task, initialDelay, period, unit);
  }

  @Override
  public void cancelAll() {
    global.cancelAll(plugin);
  }
}
