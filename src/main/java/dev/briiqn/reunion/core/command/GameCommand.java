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

package dev.briiqn.reunion.core.command;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.plugin.hooks.PermissionHooks;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public abstract class GameCommand extends Command {

  public GameCommand(String name, String usage, String description) {
    super(name, usage, description);
  }

  public boolean hasPermission(ConsoleSession session) {
    if (session == null) {
      return true;
    }

    UUID uuid = session.getUuid() != null ? session.getUuid()
        : UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getPlayerName()).getBytes(
            StandardCharsets.UTF_8));

    return PermissionHooks.hasPermission(uuid, "reunion." + getName().toLowerCase());
  }

  @Override
  public final void execute(ReunionServer server, String[] args) {
    execute(server, null, args);
  }

  public abstract void execute(ReunionServer server, ConsoleSession session, String[] args);
}