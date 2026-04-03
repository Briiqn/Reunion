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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link CommandSource} representing the proxy console.
 *
 * <p>Messages sent to this source are printed via Log4j at INFO level.
 * Obtain the singleton via {@link #INSTANCE}.
 */
public final class ConsoleCommandSource implements CommandSource {

  public static final ConsoleCommandSource INSTANCE = new ConsoleCommandSource();

  private static final Logger log = LogManager.getLogger("ReunionConsole");

  private ConsoleCommandSource() {
  }

  @Override
  public void sendMessage(String message) {
    log.info(message.replaceAll("§[0-9a-fklmnor]", ""));
  }

  @Override
  public String name() {
    return "CONSOLE";
  }

  @Override
  public boolean isConsole() {
    return true;
  }

  @Override
  public boolean isPlayer() {
    return false;
  }

  /**
   * The console always has every permission.
   *
   * @param permission ignored
   * @return always {@code true}
   */
  @Override
  public boolean hasPermission(String permission) {
    return true;
  }

  @Override
  public String toString() {
    return "ConsoleCommandSource";
  }
}