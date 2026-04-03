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

import dev.briiqn.reunion.api.permission.PermissionGroup;
import dev.briiqn.reunion.api.permission.PermissionManager;
import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.Command;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PermissionCommand extends Command {

  public PermissionCommand() {
    super("perm", "perm <user|group|listgroups> ...", "Manages permissions and groups");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    if (args.length < 1) {
      sendUsage();
      return;
    }

    PermissionManager pm = server.getPluginApi().permissionManager();

    switch (args[0].toLowerCase()) {
      case "listgroups" -> handleListGroups(pm);
      case "user" -> handleUser(pm, server, args);
      case "group" -> handleGroup(pm, args);
      default -> sendUsage();
    }
  }

  private void handleListGroups(PermissionManager pm) {
    log.info("Registered Permission Groups:");
    for (PermissionGroup g : pm.allGroups()) {
      log.info(" - {} (Priority: {}, Default: {})", g.name(), g.priority(), g.isDefault());
    }
  }

  private void handleUser(PermissionManager pm, ReunionServer server, String[] args) {
    if (args.length < 3) {
      log.info("Usage: perm user <name> <info|perm add/remove|group add/remove> ...");
      return;
    }

    String targetName = args[1];
    UUID uuid = getUuid(server, targetName);
    String sub = args[2].toLowerCase();

    switch (sub) {
      case "info" -> {
        log.info("User Info: {}", targetName);
        log.info(" UUID: {}", uuid);
        log.info(" Groups: {}", String.join(", ", pm.getPlayerGroups(uuid)));
        log.info(" Explicit Perms: {}", String.join(", ", pm.getPlayerPermissions(uuid)));
      }
      case "perm" -> {
        if (args.length < 5) {
          return;
        }
        String action = args[3].toLowerCase();
        String node = args[4];
        if (action.equals("add")) {
          pm.addPlayerPermission(uuid, node);
          log.info("Added permission '{}' to user {}", node, targetName);
        } else {
          pm.removePlayerPermission(uuid, node);
          log.info("Removed permission '{}' from user {}", node, targetName);
        }
      }
      case "group" -> {
        if (args.length < 5) {
          return;
        }
        String action = args[3].toLowerCase();
        String group = args[4];
        if (action.equals("add")) {
          pm.addPlayerToGroup(uuid, group);
          log.info("Added user {} to group {}", targetName, group);
        } else {
          pm.removePlayerFromGroup(uuid, group);
          log.info("Removed user {} from group {}", targetName, group);
        }
      }
    }
  }

  private void handleGroup(PermissionManager pm, String[] args) {
    if (args.length < 3) {
      log.info(
          "Usage: perm group <name> <create|delete|info|perm add/remove|parent add/remove|setpriority|setprefix|setdefault> ...");
      return;
    }

    String name = args[1].toLowerCase();
    String sub = args[2].toLowerCase();

    if (sub.equals("create")) {
      int priority = args.length > 3 ? Integer.parseInt(args[3]) : 0;
      pm.createGroup(name, priority);
      log.info("Group '{}' created with priority {}.", name, priority);
      return;
    }

    Optional<PermissionGroup> groupOpt = pm.getGroup(name);
    if (groupOpt.isEmpty()) {
      log.info("Group '{}' does not exist.", name);
      return;
    }

    switch (sub) {
      case "delete" -> {
        pm.deleteGroup(name);
        log.info("Group '{}' deleted.", name);
      }
      case "info" -> {
        PermissionGroup g = groupOpt.get();
        log.info("Group Info: {}", g.name());
        log.info(" Priority: {}", g.priority());
        log.info(" Prefix: '{}'", g.prefix());
        log.info(" Suffix: '{}'", g.suffix());
        log.info(" Default: {}", g.isDefault());
        log.info(" Parents: {}", String.join(", ", g.inheritedGroups()));
        log.info(" Perms: {}", String.join(", ", g.permissions()));
      }
      case "perm" -> {
        if (args.length < 5) {
          return;
        }
        String node = args[4];
        if (args[3].equalsIgnoreCase("add")) {
          pm.addGroupPermission(name, node);
          log.info("Added perm '{}' to group {}", node, name);
        } else {
          pm.removeGroupPermission(name, node);
          log.info("Removed perm '{}' from group {}", node, name);
        }
      }
      case "parent" -> {
        if (args.length < 5) {
          return;
        }
        String parent = args[4];
        if (args[3].equalsIgnoreCase("add")) {
          pm.addGroupInheritance(name, parent);
          log.info("Group {} now inherits from {}", name, parent);
        } else {
          pm.removeGroupInheritance(name, parent);
          log.info("Group {} no longer inherits from {}", name, parent);
        }
      }
      case "setpriority" -> {
        int p = Integer.parseInt(args[3]);
        pm.createGroup(name,
            p);
        log.info("Group {} priority set to {}", name, p);
      }
      case "setprefix" -> {
        String prefix = args[3];
        pm.setGroupPrefix(name, prefix);
        log.info("Group {} prefix set to '{}'", name, prefix);
      }
      case "setdefault" -> {
        boolean def = Boolean.parseBoolean(args[3]);
        pm.setGroupDefault(name, def);
        log.info("Group {} default status set to {}", name, def);
      }
    }
  }

  private UUID getUuid(ReunionServer server, String name) {
    ConsoleSession session = server.getSession(name);
    if (session != null && session.getUuid() != null) {
      return session.getUuid();
    }
    return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
  }

  private void sendUsage() {
    log.info("Permissions Command Usage:");
    log.info(" - perm listgroups");
    log.info(" - perm user <name> info");
    log.info(" - perm user <name> perm <add|remove> <node>");
    log.info(" - perm user <name> group <add|remove> <group>");
    log.info(" - perm group <name> create [priority]");
    log.info(" - perm group <name> info");
    log.info(" - perm group <name> perm <add|remove> <node>");
    log.info(" - perm group <name> setpriority <val>");
    log.info(" - perm group <name> setprefix <prefix>");
    log.info(" - perm group <name> setdefault <true|false>");
    log.info(" - perm group <name> delete");
  }
}