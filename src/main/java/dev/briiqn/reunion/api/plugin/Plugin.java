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

/**
 * The base interface for all Reunion plugins.
 *
 * <p>Plugin classes should NOT implement this interface directly. Instead, annotate your
 * main class with {@link ReunionPlugin} and extend {@link ReunionPluginBase} for lifecycle
 * convenience methods.
 *
 * <p>Example:
 * <pre>{@code
 * @ReunionPlugin(
 *     id = "myplugin",
 *     name = "My Plugin",
 *     version = "1.0.0",
 *     authors = {"YourName"},
 *     description = "Does cool things"
 * )
 * public class MyPlugin extends ReunionPluginBase {
 *     @Override
 *     public void onEnable() {
 *         logger().info("MyPlugin enabled!");
 *     }
 * }
 * }</pre>
 */
public interface Plugin {

  /**
   * Called when the plugin is being enabled. Register events, commands, and scheduled tasks here.
   */
  void onEnable();

  /**
   * Called when the plugin is being disabled. Clean up resources, cancel tasks, and save data
   * here.
   */
  void onDisable();
}
