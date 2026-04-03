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

package dev.briiqn.reunion.api.command;

import dev.briiqn.reunion.api.plugin.Plugin;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of {@link CommandManager}.
 */
public final class SimpleCommandManager implements CommandManager {

  private static final Logger log = LogManager.getLogger(SimpleCommandManager.class);

  private final Map<String, CommandRegistration> byAlias = new ConcurrentHashMap<>();
  private final Map<Plugin, Set<String>> pluginCommands = new ConcurrentHashMap<>();

  @Override
  public void register(Plugin plugin, CommandMeta meta, SimpleCommand command) {
    for (String alias : meta.aliases()) {
      if (byAlias.containsKey(alias)) {
        throw new IllegalArgumentException("Command alias '" + alias + "' is already registered.");
      }
    }
    CommandRegistration reg = new CommandRegistration(plugin, meta, command);
    for (String alias : meta.aliases()) {
      byAlias.put(alias, reg);
    }
    pluginCommands.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(meta.name());
    log.debug("[CommandManager] Registered command '{}' for plugin '{}'",
        meta.name(), plugin.getClass().getSimpleName());
  }

  @Override
  public void unregister(String name) {
    CommandRegistration reg = byAlias.remove(name.toLowerCase());
    if (reg == null) {
      return;
    }
    for (String alias : reg.meta().aliases()) {
      byAlias.remove(alias);
    }
    Set<String> owned = pluginCommands.get(reg.plugin());
    if (owned != null) {
      owned.remove(reg.meta().name());
    }
  }

  @Override
  public void unregisterAll(Plugin plugin) {
    Set<String> owned = pluginCommands.remove(plugin);
    if (owned == null) {
      return;
    }
    for (String name : owned) {
      CommandRegistration reg = byAlias.get(name);
      if (reg != null) {
        for (String alias : reg.meta().aliases()) {
          byAlias.remove(alias);
        }
      }
    }
  }

  @Override
  public boolean hasCommand(String name) {
    return byAlias.containsKey(name.toLowerCase());
  }

  @Override
  public Set<String> registeredCommands() {
    Set<String> names = new LinkedHashSet<>();
    for (CommandRegistration reg : byAlias.values()) {
      names.add(reg.meta().name());
    }
    return Collections.unmodifiableSet(names);
  }

  @Override
  public boolean dispatch(CommandSource source, String input) {
    if (input == null || input.isBlank()) {
      return false;
    }
    String[] parts = input.trim().split(" ", 2);
    String label = parts[0].toLowerCase();
    String rawArgs = parts.length > 1 ? parts[1] : "";
    String[] args = rawArgs.isEmpty() ? new String[0] : rawArgs.split(" ");

    CommandRegistration reg = byAlias.get(label);
    if (reg == null) {
      return false;
    }

    if (!reg.command().hasPermission(source)) {
      source.sendMessage("You do not have permission to use this command.");
      return true;
    }

    SimpleCommand.Invocation inv = new InvocationImpl(source, label, args, rawArgs);
    try {
      reg.command().execute(inv);
    } catch (Exception e) {
      log.error("[CommandManager] Exception executing command '{}': {}", label, e.getMessage(), e);
      source.sendMessage("An internal error occurred while executing that command.");
    }
    return true;
  }

  @Override
  public List<String> suggest(CommandSource source, String partial) {
    if (partial == null || partial.isBlank()) {
      return registeredCommands().stream().sorted().toList();
    }
    String[] parts = partial.split(" ", 2);
    String label = parts[0].toLowerCase();

    if (parts.length == 1) {
      return byAlias.keySet().stream()
          .filter(k -> k.startsWith(label))
          .sorted()
          .toList();
    }

    CommandRegistration reg = byAlias.get(label);
    if (reg == null || !reg.command().hasPermission(source)) {
      return Collections.emptyList();
    }

    String rawArgs = parts[1];
    String[] args = rawArgs.split(" ", -1);
    SimpleCommand.Invocation inv = new InvocationImpl(source, label, args, rawArgs);
    return reg.command().suggest(inv);
  }


  private record CommandRegistration(Plugin plugin, CommandMeta meta, SimpleCommand command) {

  }

  private record InvocationImpl(
      CommandSource source,
      String alias,
      String[] arguments,
      String rawArguments
  ) implements SimpleCommand.Invocation {

  }
}
