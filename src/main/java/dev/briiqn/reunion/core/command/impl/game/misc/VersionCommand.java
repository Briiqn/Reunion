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
import dev.briiqn.reunion.core.session.ConsoleSession;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VersionCommand extends GameCommand {

  public VersionCommand() {
    super("version", "ver", "Get the current proxy version");
  }

  @Override
  public void execute(ReunionServer server, ConsoleSession session, String[] args) {
    if (session != null) {
      session.queueOrSendChat("Running Reunion " + ReunionServer.releaseVersion);
      session.queueOrSendChat(
          "This software is developed by Briiqn and is licensed under the GNU Affero General Public License");

    } else {
      log.info("Running Reunion " + ReunionServer.releaseVersion);
      log.info(
          "This software is developed by Briiqn and is licensed under the GNU Affero General Public License");
    }
  }
}
