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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Manages plugin loading, unloading, and lookup.
 */
public interface PluginManager {

  /**
   * Returns all currently loaded plugins.
   */
  Collection<Plugin> plugins();

  /**
   * Looks up a loaded plugin by its ID (from {@code @ReunionPlugin#id}).
   *
   * @param id the plugin ID (case-sensitive)
   * @return an Optional containing the plugin instance, or empty if not found
   */
  Optional<Plugin> plugin(String id);

  /**
   * Looks up a loaded plugin's description by its ID.
   *
   * @param id the plugin ID
   * @return an Optional containing the description, or empty if not found
   */
  Optional<PluginDescription> description(String id);

  /**
   * Returns {@code true} if a plugin with the given ID is currently loaded.
   */
  boolean isLoaded(String id);

  /**
   * Loads and enables all valid plugin JARs in the given directory. Called automatically by Reunion
   * on startup.
   *
   * @param directory the directory to scan for plugin JARs
   */
  void loadAll(Path directory);

  /**
   * Loads a single plugin JAR.
   *
   * @param jar path to the JAR file
   * @return the loaded plugin
   * @throws PluginLoadException if the JAR is invalid or the plugin fails to load
   */
  Plugin load(Path jar) throws PluginLoadException;

  /**
   * Disables and unloads a plugin by ID. All events and tasks registered by the plugin are
   * automatically cleaned up.
   *
   * @param id the plugin ID
   * @throws IllegalArgumentException if no plugin with that ID is loaded
   */
  void unload(String id);
}
