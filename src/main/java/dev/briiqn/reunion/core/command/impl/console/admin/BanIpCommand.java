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
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.net.InetSocketAddress;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BanIpCommand extends Command {

  public BanIpCommand() {
    super("banip", "banip <player|ip> [reason]", "Bans an IP address from the proxy");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    if (args.length < 1) {
      log.info("Usage: " + getUsage());
      return;
    }

    String target = args[0];
    String reason = args.length > 1 ? String.join(" ", args).substring(target.length() + 1)
        : "Banned by operator";

    String ip = resolveIp(server, target);
    if (ip == null) {
      log.info("Could not resolve IP for: {}", target);
      return;
    }

    server.getBanManager().banIp(ip, reason);
    log.info("Banned IP: {} ({})", ip, reason);

    for (ConsoleSession session : server.getSessions().values()) {
      String sessionIp = ((InetSocketAddress) session.getConsoleChannel()
          .remoteAddress()).getAddress().getHostAddress();
      if (sessionIp.equals(ip)) {
        PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
        session.getConsoleChannel().close();
        log.info("Kicked {} due to IP ban", session.getPlayerName());
      }
    }
  }

  private String resolveIp(ReunionServer server, String target) {
    ConsoleSession session = server.getSession(target);
    if (session != null) {
      return ((InetSocketAddress) session.getConsoleChannel().remoteAddress()).getAddress()
          .getHostAddress();
    }
    // {}.{}.{}.{}. regex
    if (target.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
      return target;
    }
    return null;
  }
}