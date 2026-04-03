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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Runtime metadata for a loaded plugin. Constructed from the {@code @ReunionPlugin} annotation by
 * the plugin loader.
 */
public final class PluginDescription {

  private final String id;
  private final String name;
  private final String version;
  private final String description;
  private final List<String> authors;
  private final List<String> depends;
  private final List<String> softDepends;
  private final String url;

  public PluginDescription(String id, String name, String version, String description,
      String[] authors, String[] depends, String[] softDepends, String url) {
    this.id = id;
    this.name = name.isEmpty() ? id : name;
    this.version = version;
    this.description = description;
    this.authors = Collections.unmodifiableList(Arrays.asList(authors));
    this.depends = Collections.unmodifiableList(Arrays.asList(depends));
    this.softDepends = Collections.unmodifiableList(Arrays.asList(softDepends));
    this.url = url;
  }

  /**
   * The plugin's unique lowercase ID.
   */
  public String id() {
    return id;
  }

  /**
   * The human-readable display name.
   */
  public String name() {
    return name;
  }

  /**
   * The version string.
   */
  public String version() {
    return version;
  }

  /**
   * Short description of the plugin.
   */
  public String description() {
    return description;
  }

  /**
   * Authors list, may be empty.
   */
  public List<String> authors() {
    return authors;
  }

  /**
   * Required dependency plugin IDs.
   */
  public List<String> depends() {
    return depends;
  }

  /**
   * Optional dependency plugin IDs.
   */
  public List<String> softDepends() {
    return softDepends;
  }

  /**
   * Plugin website or source URL.
   */
  public String url() {
    return url;
  }

  @Override
  public String toString() {
    return name + " v" + version + " by " + authors;
  }
}
