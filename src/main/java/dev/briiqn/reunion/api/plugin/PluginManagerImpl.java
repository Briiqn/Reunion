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
 * Default implementation of {@link PluginManager} backed by {@link PluginLoader}.
 */
public final class PluginManagerImpl implements PluginManager {

  private final PluginLoader loader;

  public PluginManagerImpl(PluginLoader loader) {
    this.loader = loader;
  }

  @Override
  public Collection<Plugin> plugins() {
    return loader.plugins();
  }

  @Override
  public Optional<Plugin> plugin(String id) {
    return loader.plugin(id);
  }

  @Override
  public Optional<PluginDescription> description(String id) {
    return loader.description(id);
  }

  @Override
  public boolean isLoaded(String id) {
    return loader.isLoaded(id);
  }

  @Override
  public void loadAll(Path directory) {
    loader.loadAll();
  }

  @Override
  public Plugin load(Path jar) throws PluginLoadException {
    return loader.load(jar);
  }

  @Override
  public void unload(String id) {
    loader.unload(id);
  }
}
