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

package dev.briiqn.reunion.core.command.impl.console.player;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.Command;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.net.InetSocketAddress;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PlayerInfoCommand extends Command {

  public PlayerInfoCommand() {
    super("list", "list", "Lists online players");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    var sessions = server.getSessions();
    log.info("Online Players ({}):", sessions.size());
    for (ConsoleSession session : sessions.values()) {
      String ip = "unknown";
      try {
        ip = ((InetSocketAddress) session.getConsoleChannel().remoteAddress())
            .getAddress().getHostAddress();
      } catch (Exception ignored) {
      }

      String ping = session.getPing() >= 0 ? session.getPing() + "ms" : "?";
      String srv = server.getConfig().getNetwork().isNetworkMode()
          ? " | Server: " + (session.getCurrentServer() != null ? session.getCurrentServer() : "?")
          : "";

      log.info(" - {} | {} | Ver: {} | Dim: {} | Ping: {}{}",
          session.getPlayerName(), ip,
          session.getClientVersion(), session.getDimension(), ping, srv);
    }
  }
}