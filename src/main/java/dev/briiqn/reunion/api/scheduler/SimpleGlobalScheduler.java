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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of {@link GlobalScheduler} backed by a single-thread timer with
 * virtual-thread-per-task execution for the actual work.
 */
public final class SimpleGlobalScheduler implements GlobalScheduler {

  private static final Logger log = LogManager.getLogger(SimpleGlobalScheduler.class);

  private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "reunion-scheduler-timer");
    t.setDaemon(true);
    return t;
  });

  private final Set<SimpleScheduledTask> tasks = ConcurrentHashMap.newKeySet();

  @Override
  public ScheduledTask run(Plugin plugin, Runnable task) {
    SimpleScheduledTask handle = new SimpleScheduledTask(plugin);
    tasks.add(handle);
    Thread.ofVirtual().name("reunion-task").start(() -> {
      if (!handle.isCancelled()) {
        try {
          task.run();
        } catch (Exception e) {
          log.error("[Scheduler] Unhandled exception in task for plugin {}",
              plugin.getClass().getSimpleName(), e);
        }
      }
      handle.markDone();
      tasks.remove(handle);
    });
    return handle;
  }

  @Override
  public ScheduledTask runLater(Plugin plugin, Runnable task, long delay, TimeUnit unit) {
    SimpleScheduledTask handle = new SimpleScheduledTask(plugin);
    tasks.add(handle);
    handle.setFuture(timer.schedule(() -> {
      if (!handle.isCancelled()) {
        Thread.ofVirtual().name("reunion-task-delayed").start(() -> {
          try {
            task.run();
          } catch (Exception e) {
            log.error("[Scheduler] Unhandled exception in delayed task for plugin {}",
                plugin.getClass().getSimpleName(), e);
          }
          handle.markDone();
          tasks.remove(handle);
        });
      } else {
        tasks.remove(handle);
      }
    }, delay, unit));
    return handle;
  }

  @Override
  public ScheduledTask runRepeating(Plugin plugin, Runnable task, long initialDelay, long period,
      TimeUnit unit) {
    SimpleScheduledTask handle = new SimpleScheduledTask(plugin);
    tasks.add(handle);
    handle.setFuture(timer.scheduleWithFixedDelay(() -> {
      if (handle.isCancelled()) {
        return;
      }
      Thread.ofVirtual().name("reunion-task-repeating").start(() -> {
        if (!handle.isCancelled()) {
          try {
            task.run();
          } catch (Exception e) {
            log.error("[Scheduler] Unhandled exception in repeating task for plugin {}",
                plugin.getClass().getSimpleName(), e);
          }
        }
      });
    }, initialDelay, period, unit));
    return handle;
  }

  @Override
  public void cancelAll(Plugin plugin) {
    tasks.removeIf(task -> {
      if (task.plugin() == plugin) {
        task.cancel();
        return true;
      }
      return false;
    });
  }

  @Override
  public void shutdown() {
    tasks.forEach(SimpleScheduledTask::cancel);
    tasks.clear();
    timer.shutdownNow();
  }
}
