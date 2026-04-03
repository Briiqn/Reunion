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

package dev.briiqn.reunion.core.command.impl.console;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.Command;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HelpCommand extends Command {

  public HelpCommand() {
    super("help", "help", "Lists all available commands");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    log.info("Available Commands:");
    log.info("--------------------------------------------------");

    int maxUsageLength = 0;
    for (Command cmd : server.getCommandManager().getCommands()) {
      if (cmd.getUsage().length() > maxUsageLength) {
        maxUsageLength = cmd.getUsage().length();
      }
    }

    for (Command cmd : server.getCommandManager().getCommands()) {
      String padding = " ".repeat(Math.max(0, maxUsageLength - cmd.getUsage().length()));
      log.info("{} {} - {}", cmd.getUsage(), padding, cmd.getDescription());
    }
    log.info("--------------------------------------------------");
  }
}