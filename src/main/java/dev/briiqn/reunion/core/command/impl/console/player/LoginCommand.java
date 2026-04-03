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
import dev.briiqn.reunion.core.util.auth.AuthUtil;
import lombok.extern.log4j.Log4j2;
import net.raphimc.minecraftauth.java.JavaAuthManager;

@Log4j2
public class LoginCommand extends Command {

  public LoginCommand() {
    super("login", "login [logout]", "Manage the Minecraft account session");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    if (args.length > 0 && args[0].equalsIgnoreCase("logout")) {
      try {
        AuthUtil.logout();
        log.info("Logged out and session cleared.");
      } catch (Exception e) {
        log.error("Failed to logout: {}", e.getMessage());
      }
      return;
    }

    if (AuthUtil.hasSession()) {
      log.info("Already logged in. Use 'login logout' to clear the session.");
      return;
    }

    log.info("Starting Microsoft login...");
    Thread.ofVirtual().name("mc-login").start(() -> {
      try {
        JavaAuthManager session = AuthUtil.login();
        log.info("Successfully logged in.");
      } catch (Exception e) {
        log.error("Login failed: {}", e.getMessage());
      }
    });
  }
}