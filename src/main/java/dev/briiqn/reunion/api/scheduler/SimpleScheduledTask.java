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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default {@link ScheduledTask} implementation used by {@link SimpleGlobalScheduler}.
 */
public final class SimpleScheduledTask implements ScheduledTask {

  private final Plugin plugin;
  private final AtomicBoolean cancelled = new AtomicBoolean(false);
  private final AtomicBoolean done = new AtomicBoolean(false);
  private volatile Future<?> future;

  public SimpleScheduledTask(Plugin plugin) {
    this.plugin = plugin;
  }

  public void setFuture(Future<?> future) {
    this.future = future;
    if (cancelled.get()) {
      future.cancel(false);
    }
  }

  public void markDone() {
    done.set(true);
  }

  @Override
  public void cancel() {
    cancelled.set(true);
    if (future != null) {
      future.cancel(false);
    }
  }

  @Override
  public boolean isCancelled() {
    return cancelled.get();
  }

  @Override
  public boolean isDone() {
    return done.get();
  }

  @Override
  public Plugin plugin() {
    return plugin;
  }
}
