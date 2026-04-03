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

package dev.briiqn.reunion.api.plugin;

import dev.briiqn.reunion.api.ReunionProxy;
import dev.briiqn.reunion.api.scheduler.PluginScheduler;
import org.apache.logging.log4j.Logger;

/**
 * Convenient abstract base class for plugin main classes.
 *
 * <p>Extend this class and annotate it with {@code @ReunionPlugin}.
 * Override {@link #onEnable()} and {@link #onDisable()} as needed.
 *
 * <p>The proxy, logger, scheduler, and data folder are injected by the
 * plugin loader before {@link #onEnable()} is called.
 */
public abstract class ReunionPluginBase implements Plugin {

  private ReunionProxy proxy;
  private Logger logger;
  private PluginScheduler scheduler;
  private java.io.File dataFolder;
  private PluginDescription description;

  /**
   * Injected by the plugin loader  do not call before {@link #onEnable()}.
   */
  public final ReunionProxy proxy() {
    return proxy;
  }

  /**
   * A logger scoped to this plugin.
   */
  public final Logger logger() {
    return logger;
  }

  /**
   * A scheduler scoped to this plugin. All tasks registered here are automatically cancelled when
   * the plugin is disabled.
   */
  public final PluginScheduler scheduler() {
    return scheduler;
  }

  /**
   * The directory where this plugin may store persistent data. Created lazily on first access.
   */
  public final java.io.File dataFolder() {
    if (!dataFolder.exists()) {
      dataFolder.mkdirs();
    }
    return dataFolder;
  }

  /**
   * The resolved description for this plugin, derived from the {@code @ReunionPlugin} annotation.
   */
  public final PluginDescription pluginDescription() {
    return description;
  }


  @Override
  public void onEnable() {
  }

  @Override
  public void onDisable() {
  }


  /**
   * @hidden
   */
  public final void inject(ReunionProxy proxy, Logger logger, PluginScheduler scheduler,
      java.io.File dataFolder, PluginDescription description) {
    this.proxy = proxy;
    this.logger = logger;
    this.scheduler = scheduler;
    this.dataFolder = dataFolder;
    this.description = description;
  }
}
