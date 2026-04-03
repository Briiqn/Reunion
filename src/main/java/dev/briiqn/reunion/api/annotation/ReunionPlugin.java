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

package dev.briiqn.reunion.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the metadata for a Reunion plugin.
 *
 * <p>Place this annotation on your plugin's main class (the entrypoint declared in
 * {@code plugin.yml}). All plugin metadata is handled here  no need to duplicate it in the YAML
 * file.
 *
 * <p>{@code plugin.yml} only needs to declare the {@code main} class:
 * <pre>
 * main: com.example.MyPlugin
 * </pre>
 *
 * <p>Everything else (name, version, authors, dependencies, etc.) is read from
 * this annotation at load time.
 *
 * <p>Example:
 * <pre>{@code
 * @ReunionPlugin(
 *     id          = "my-plugin",
 *     name        = "My Plugin",
 *     version     = "1.0.0",
 *     description = "A cool plugin",
 *     authors     = {"Alice", "Bob"},
 *     depends     = {"other-plugin"}
 * )
 * public class MyPlugin extends ReunionPluginBase { ... }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ReunionPlugin {

  /**
   * The unique identifier for this plugin. Must be lowercase, alphanumeric, and may contain
   * hyphens. This ID is used for dependency declarations and plugin lookups.
   */
  String id();

  /**
   * The human-readable display name of the plugin. Defaults to the plugin ID if not set.
   */
  String name() default "";

  /**
   * The plugin version string (e.g., "1.0.0", "2.3.1-SNAPSHOT").
   */
  String version() default "1.0.0";

  /**
   * A short description of what the plugin does.
   */
  String description() default "";

  /**
   * The list of authors who created this plugin.
   */
  String[] authors() default {};

  /**
   * Plugin IDs that must be loaded before this plugin. If a dependency is absent, this plugin will
   * fail to load.
   */
  String[] depends() default {};

  /**
   * Plugin IDs that should be loaded before this plugin if present, but are not required. Soft
   * dependencies will not cause a load failure.
   */
  String[] softDepends() default {};

  /**
   * The plugin's website or source repository URL.
   */
  String url() default "";
}
