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

package dev.briiqn.reunion.core.command.impl.console.admin;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.Command;
import dev.briiqn.reunion.core.config.ServerEntry;
import dev.briiqn.reunion.core.network.server.ServerSwitchHandler;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerCommand extends Command {

  public ServerCommand() {
    super("server", "server <player> <server> | server list",
        "Switch a player to a backend server");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    if (!server.getConfig().getNetwork().isNetworkMode()) {
      log.info("Network mode is not enabled. Set network-mode: true in config.yml.");
      return;
    }

    Map<String, ServerEntry> servers = server.getConfig().getNetwork().getServers();

    if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
      log.info("Servers:");
      for (ServerEntry e : servers.values()) {
        log.info("  {} -> {}:{}", e.name(), e.host(), e.port());
      }
      return;
    }

    if (args.length < 2) {
      log.info("Usage: " + getUsage());
      return;
    }

    String playerName = args[0];
    String targetName = args[1];

    ConsoleSession session = server.getSession(playerName);
    if (session == null) {
      log.info("Player {} not found.", playerName);
      return;
    }

    ServerEntry target = server.getConfig().getServer(targetName);
    if (target == null) {
      log.info("Unknown server: {}. Available: {}", targetName,
          String.join(", ", servers.keySet()));
      return;
    }

    if (targetName.equalsIgnoreCase(session.getCurrentServer())) {
      session.queueOrSendChat("[Reunion] You are already connected to this server.");
      return;
    }

    log.info("Sending {} to '{}'...", playerName, targetName);
    ServerSwitchHandler.switchServer(session, target);
  }
}