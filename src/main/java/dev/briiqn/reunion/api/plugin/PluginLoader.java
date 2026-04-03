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
import dev.briiqn.reunion.api.annotation.ReunionPlugin;
import dev.briiqn.reunion.api.scheduler.GlobalScheduler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads plugin JARs from a directory.
 *
 * <p>{@code plugin.yml} needs only one key:
 * <pre>main: com.example.myplugin.MyPlugin</pre>
 * <p>
 * Everything else is read from {@code @ReunionPlugin} on the main class.
 */
public final class PluginLoader {

  private static final Logger log = LogManager.getLogger(PluginLoader.class);

  private final ReunionProxy proxy;
  private final GlobalScheduler globalScheduler;
  private final Path pluginsDir;
  private final File pluginsDataDir;

  private final Map<String, PluginEntry> loaded = new ConcurrentHashMap<>();

  public PluginLoader(ReunionProxy proxy, GlobalScheduler globalScheduler, Path pluginsDir) {
    this.proxy = proxy;
    this.globalScheduler = globalScheduler;
    this.pluginsDir = pluginsDir;
    this.pluginsDataDir = pluginsDir.toFile();
  }


  public void loadAll() {
    if (!Files.isDirectory(pluginsDir)) {
      try {
        Files.createDirectories(pluginsDir);
      } catch (IOException e) {
        log.error("[PluginLoader] Could not create plugins directory: {}", e.getMessage());
        return;
      }
    }

    List<Path> jars = new ArrayList<>();
    try (Stream<Path> stream = Files.list(pluginsDir)) {
      stream.filter(p -> p.toString().endsWith(".jar")).forEach(jars::add);
    } catch (IOException e) {
      log.error("[PluginLoader] Failed to list plugins directory: {}", e.getMessage());
      return;
    }

    List<PluginEntry> discovered = new ArrayList<>();
    for (Path jar : jars) {
      try {
        PluginEntry entry = discover(jar);
        if (entry != null) {
          discovered.add(entry);
        }
      } catch (Exception e) {
        log.error("[PluginLoader] Failed to discover plugin at {}: {}", jar.getFileName(),
            e.getMessage());
      }
    }

    List<PluginEntry> sorted = sort(discovered);

    for (PluginEntry entry : sorted) {
      try {
        enable(entry);
      } catch (Exception e) {
        log.error("[PluginLoader] Failed to enable plugin {}: {}", entry.description.id(),
            e.getMessage(), e);
      }
    }
  }

  public Plugin load(Path jar) {
    try {
      PluginEntry entry = discover(jar);
      if (entry == null) {
        throw new PluginLoadException("No valid plugin.yml in " + jar.getFileName());
      }
      enable(entry);
      return entry.instance;
    } catch (PluginLoadException e) {
      throw e;
    } catch (Exception e) {
      throw new PluginLoadException("Failed to load " + jar.getFileName(), e);
    }
  }

  public void unload(String id) {
    PluginEntry entry = loaded.remove(id);
    if (entry == null) {
      throw new IllegalArgumentException("No plugin loaded with id: " + id);
    }
    disable(entry);
  }

  public Collection<Plugin> plugins() {
    List<Plugin> result = new ArrayList<>(loaded.size());
    for (PluginEntry entry : loaded.values()) {
      if (entry.instance != null) {
        result.add(entry.instance);
      }
    }
    return Collections.unmodifiableList(result);
  }

  public Optional<Plugin> plugin(String id) {
    PluginEntry e = loaded.get(id);
    return e == null ? Optional.empty() : Optional.of(e.instance);
  }

  public Optional<PluginDescription> description(String id) {
    PluginEntry e = loaded.get(id);
    return e == null ? Optional.empty() : Optional.of(e.description);
  }

  public boolean isLoaded(String id) {
    return loaded.containsKey(id);
  }


  private PluginEntry discover(Path jar) throws Exception {
    String mainClass = readMainClass(jar);
    if (mainClass == null) {
      log.warn("[PluginLoader] {} has no plugin.yml or missing 'main' key, skipping.",
          jar.getFileName());
      return null;
    }

    URLClassLoader cl = new URLClassLoader(new URL[]{jar.toUri().toURL()},
        getClass().getClassLoader());
    Class<?> clazz = cl.loadClass(mainClass);

    ReunionPlugin annotation = clazz.getAnnotation(ReunionPlugin.class);
    if (annotation == null) {
      log.warn("[PluginLoader] Main class {} is missing @ReunionPlugin skipping.", mainClass);
      cl.close();
      return null;
    }

    if (!ReunionPluginBase.class.isAssignableFrom(clazz)) {
      log.warn("[PluginLoader] Main class {} does not extend ReunionPluginBase skipping.",
          mainClass);
      cl.close();
      return null;
    }

    PluginDescription desc = new PluginDescription(
        annotation.id(), annotation.name(), annotation.version(), annotation.description(),
        annotation.authors(), annotation.depends(), annotation.softDepends(), annotation.url()
    );
    return new PluginEntry(jar, cl, clazz, desc, null);
  }

  private void enable(PluginEntry entry) throws Exception {
    for (String dep : entry.description.depends()) {
      if (!loaded.containsKey(dep)) {
        throw new PluginLoadException("Plugin '" + entry.description.id()
            + "' requires dependency '" + dep + "' which is not loaded.");
      }
    }

    ReunionPluginBase instance = (ReunionPluginBase) entry.clazz.getDeclaredConstructor()
        .newInstance();
    org.apache.logging.log4j.Logger logger = LogManager.getLogger(entry.description.name());
    File dataFolder = new File(pluginsDataDir, entry.description.id());
    var pluginScheduler = globalScheduler.forPlugin(instance);

    instance.inject(proxy, logger, pluginScheduler, dataFolder, entry.description);

    log.info("[PluginLoader] Enabling {} v{}", entry.description.name(),
        entry.description.version());
    instance.onEnable();
    proxy.eventBus().register(instance, instance);
    loaded.put(entry.description.id(), entry.withInstance(instance));
    log.info("[PluginLoader] Enabled {} v{}", entry.description.name(),
        entry.description.version());
  }

  private void disable(PluginEntry entry) {
    if (entry.instance == null) {
      return;
    }
    log.info("[PluginLoader] Disabling {} v{}", entry.description.name(),
        entry.description.version());
    try {
      entry.instance.onDisable();
    } catch (Exception e) {
      log.error("[PluginLoader] Exception in onDisable for {}: {}", entry.description.name(),
          e.getMessage(), e);
    }
    proxy.eventBus().unregisterAll(entry.instance);
    proxy.commandManager().unregisterAll(entry.instance);
    proxy.messagingRegistry().unregisterAll(entry.instance);
    globalScheduler.cancelAll(entry.instance);
    try {
      entry.classLoader.close();
    } catch (IOException e) {
      log.warn("[PluginLoader] Failed to close ClassLoader for {}", entry.description.name());
    }
    log.info("[PluginLoader] Disabled {}", entry.description.name());
  }

  private String readMainClass(Path jar) {
    try (JarFile jf = new JarFile(jar.toFile())) {
      var entry = jf.getEntry("plugin.yml");
      if (entry == null) {
        return null;
      }
      try (InputStream is = jf.getInputStream(entry)) {
        Map<String, Object> data = new Yaml().load(is);
        if (data == null) {
          return null;
        }
        Object main = data.get("main");
        return main instanceof String s ? s : null;
      }
    } catch (IOException e) {
      log.warn("[PluginLoader] Could not read plugin.yml from {}: {}", jar.getFileName(),
          e.getMessage());
      return null;
    }
  }

  private List<PluginEntry> sort(List<PluginEntry> entries) {
    Map<String, PluginEntry> byId = new LinkedHashMap<>();
    for (PluginEntry e : entries) {
      byId.put(e.description.id(), e);
    }
    List<PluginEntry> result = new ArrayList<>();
    Set<String> visited = new LinkedHashSet<>();
    Set<String> visiting = new LinkedHashSet<>();
    for (String id : byId.keySet()) {
      visit(id, byId, visited, visiting, result);
    }
    return result;
  }

  private void visit(String id, Map<String, PluginEntry> byId,
      Set<String> visited, Set<String> visiting, List<PluginEntry> result) {
    if (visited.contains(id)) {
      return;
    }
    if (visiting.contains(id)) {
      log.warn("[PluginLoader] Circular dependency involving '{}'  skipping.", id);
      return;
    }
    PluginEntry entry = byId.get(id);
    if (entry == null) {
      return;
    }
    visiting.add(id);
    for (String dep : entry.description.depends()) {
      visit(dep, byId, visited, visiting, result);
    }
    for (String soft : entry.description.softDepends()) {
      visit(soft, byId, visited, visiting, result);
    }
    visiting.remove(id);
    visited.add(id);
    result.add(entry);
  }

  private record PluginEntry(
      Path jar, URLClassLoader classLoader, Class<?> clazz,
      PluginDescription description, ReunionPluginBase instance
  ) {

    PluginEntry withInstance(ReunionPluginBase inst) {
      return new PluginEntry(jar, classLoader, clazz, description, inst);
    }
  }
}
