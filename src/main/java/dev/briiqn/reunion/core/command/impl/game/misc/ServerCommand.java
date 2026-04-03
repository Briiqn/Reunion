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

package dev.briiqn.reunion.core.command.impl.game.misc;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.GameCommand;
import dev.briiqn.reunion.core.config.ServerEntry;
import dev.briiqn.reunion.core.network.server.ServerSwitchHandler;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.util.Map;
import java.util.StringJoiner;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerCommand extends GameCommand {

  public ServerCommand() {
    super("server", "server [player] <server> | server list",
        "Switch to a backend server");
  }

  @Override
  public void execute(ReunionServer server, ConsoleSession session, String[] args) {
    if (!server.getConfig().getNetwork().isNetworkMode()) {
      session.queueOrSendChat("Network mode is not enabled. Set network-mode: true in config.yml.");
      return;
    }

    Map<String, ServerEntry> servers = server.getConfig().getNetwork().getServers();

    if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
      if (session == null) {
        log.info("Servers:");
        for (ServerEntry e : servers.values()) {
          log.info("  {} -> {}:{}", e.name(), e.host(), e.port());
        }
      } else {
        StringJoiner sj = new StringJoiner(", ");
        for (String name : servers.keySet()) {
          sj.add(name.equals(session.getCurrentServer()) ? "[" + name + "]" : name);
        }
        session.queueOrSendChat("[Reunion] Servers: " + sj);
      }
      return;
    }

    String targetName;
    ConsoleSession targetSession = session;

    if (session == null) {
      if (args.length < 2) {
        log.info("Usage: server <player> <server> | server list");
        return;
      }
      targetSession = server.getSession(args[0]);
      targetName = args[1];
      if (targetSession == null) {
        log.info("Player {} not found.", args[0]);
        return;
      }
    } else {
      targetName = args[0];
    }

    ServerEntry target = server.getConfig().getServer(targetName);
    if (target == null) {
      session.queueOrSendChat("[Reunion] Unknown server: " + targetName);
      return;
    }

    if (targetName.equalsIgnoreCase(targetSession.getCurrentServer())) {
      session.queueOrSendChat(
          "[Reunion] " + (session == null ? targetSession.getPlayerName() + " is" : "You are")
              + " already connected to this server.");
      return;
    }

    if (session == null) {
      log.info("Sending {} to '{}'...", targetSession.getPlayerName(), targetName);
    }
    ServerSwitchHandler.switchServer(targetSession, target);
  }


}