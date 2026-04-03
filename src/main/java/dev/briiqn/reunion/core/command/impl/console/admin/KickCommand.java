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
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KickCommand extends Command {

  public KickCommand() {
    super("kick", "kick <player>", "Disconnects a player");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    if (args.length < 1) {
      log.info("Usage: " + getUsage());
      return;
    }
    String target = args[0];
    ConsoleSession session = server.getSession(target);
    if (session != null) {
      PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
      session.getConsoleChannel().close();
      log.info("Kicked player {}", target);
    } else {
      log.info("Player {} not found.", target);
    }
  }
}