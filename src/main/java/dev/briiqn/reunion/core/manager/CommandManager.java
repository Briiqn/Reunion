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

package dev.briiqn.reunion.core.manager;

import dev.briiqn.reunion.api.command.CommandSource;
import dev.briiqn.reunion.api.command.ConsoleCommandSource;
import dev.briiqn.reunion.api.command.PlayerCommandSource;
import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.Command;
import dev.briiqn.reunion.core.command.GameCommand;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommandManager {

  private final ReunionServer server;
  private final Map<String, Command> commands = new TreeMap<>();

  public CommandManager(ReunionServer server) {
    this.server = server;
    scan("dev.briiqn.reunion.core.command.impl");
  }

  private void scan(String packageName) {
    try (ScanResult result = new ClassGraph()
        .enableClassInfo()
        .acceptPackages(packageName)
        .scan()) {

      for (ClassInfo ci : result.getSubclasses(Command.class.getName())) {
        if (ci.isAbstract()) {
          continue;
        }
        try {
          Class<?> clazz = ci.loadClass();
          Command cmd = (Command) clazz.getDeclaredConstructor().newInstance();
          register(cmd);
        } catch (Exception e) {
          log.error("Failed to load command: " + ci.getName(), e);
        }
      }
    }
  }

  public void register(Command command) {
    commands.put(command.getName().toLowerCase(), command);
  }

  public Collection<Command> getCommands() {
    return commands.values();
  }

  public boolean dispatch(ConsoleSession session, String line) {
    if (line == null || line.isBlank()) {
      return false;
    }

    String trimmed = line.trim();
    String inputForParsing = trimmed;
    if (session != null && inputForParsing.startsWith("/")) {
      inputForParsing = inputForParsing.substring(1);
    }

    String[] parts = inputForParsing.split(" ");
    String name = parts[0].toLowerCase();
    Command cmd = commands.get(name);

    if (cmd != null) {
      if (session != null && !(cmd instanceof GameCommand)) {
        return false;
      }
      String[] args = new String[parts.length - 1];
      System.arraycopy(parts, 1, args, 0, args.length);
      try {
        if (cmd instanceof GameCommand gc) {
          if (session != null && !gc.hasPermission(session)) {
            session.queueOrSendChat("You do not have permission to use this command.");
            return true;
          }
          gc.execute(server, session, args);
        } else {
          cmd.execute(server, args);
        }
      } catch (Exception e) {
        log.error("Error executing command '{}': {}", name, e.getMessage(), e);
      }
      return true;
    }

    CommandSource source;
    if (session == null) {
      source = ConsoleCommandSource.INSTANCE;
    } else {
      source = new PlayerCommandSource(new ConsoleSessionPlayerAdapter(session));
    }

    if (server.getPluginApi() != null &&
        server.getPluginApi().rawCommandManager().dispatch(source, inputForParsing)) {
      return true;
    }

    if (session == null) {
      log.info("Unknown command '{}'. Type 'help' for a list of commands.", name);
      return true;
    }

    return false;
  }

  public void dispatch(String line) {
    dispatch(null, line);
  }
}