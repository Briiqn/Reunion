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

package dev.briiqn.reunion.core.permission;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.briiqn.reunion.api.permission.PermissionGroup;
import dev.briiqn.reunion.api.permission.PermissionManager;
import dev.briiqn.reunion.core.permission.data.PermissionDao;
import dev.briiqn.reunion.core.permission.data.PermissionGroupRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

@Log4j2
public final class PermissionManagerImpl implements PermissionManager {

  @Getter(AccessLevel.PACKAGE)
  public final Jdbi jdbi;
  private final HikariDataSource dataSource;

  public PermissionManagerImpl(Path dataDir) {
    try {
      Files.createDirectories(dataDir);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create permission data directory: " + dataDir, e);
    }

    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl("jdbc:sqlite:" + dataDir.resolve("permissions.db").toAbsolutePath());
    cfg.setDriverClassName("org.sqlite.JDBC");
    cfg.setMaximumPoolSize(1);
    cfg.setPoolName("reunion-permissions");
    cfg.addDataSourceProperty("journal_mode", "WAL");
    cfg.addDataSourceProperty("foreign_keys", "ON");

    this.dataSource = new HikariDataSource(cfg);
    this.jdbi = Jdbi.create(dataSource);
    this.jdbi.installPlugin(new SqlObjectPlugin());

    initSchema();
    ensureDefaultGroup();
    log.info("[Permissions] Found {} groups", allGroups().size());
  }

  private void initSchema() {
    jdbi.useHandle(handle -> {
      handle.execute("PRAGMA foreign_keys = ON");
      handle.execute(
          "CREATE TABLE IF NOT EXISTS permission_groups (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, priority INTEGER NOT NULL DEFAULT 0, prefix TEXT NOT NULL DEFAULT '', suffix TEXT NOT NULL DEFAULT '', default_group INTEGER NOT NULL DEFAULT 0)");
      handle.execute(
          "CREATE TABLE IF NOT EXISTS group_permissions (group_id INTEGER NOT NULL, permission TEXT NOT NULL, PRIMARY KEY (group_id, permission), FOREIGN KEY (group_id) REFERENCES permission_groups(id) ON DELETE CASCADE)");
      handle.execute(
          "CREATE TABLE IF NOT EXISTS group_inheritance (group_id INTEGER NOT NULL, parent_id INTEGER NOT NULL, PRIMARY KEY (group_id, parent_id), FOREIGN KEY (group_id) REFERENCES permission_groups(id) ON DELETE CASCADE, FOREIGN KEY (parent_id) REFERENCES permission_groups(id) ON DELETE CASCADE)");
      handle.execute(
          "CREATE TABLE IF NOT EXISTS player_groups (uuid TEXT NOT NULL, group_id INTEGER NOT NULL, PRIMARY KEY (uuid, group_id), FOREIGN KEY (group_id) REFERENCES permission_groups(id) ON DELETE CASCADE)");
      handle.execute(
          "CREATE TABLE IF NOT EXISTS player_permissions (uuid TEXT NOT NULL, permission TEXT NOT NULL, PRIMARY KEY (uuid, permission))");
    });
  }

  private void ensureDefaultGroup() {
    if (getGroup("default").isEmpty()) {
      createGroup("default", 0);
      setGroupDefault("default", true);
      log.info("[Permissions] Created default group.");
    }
  }

  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }

  @Override
  public boolean hasPermission(UUID uuid, String permission) {
    if (permission == null || permission.isBlank()) {
      return true;
    }
    String node = permission.toLowerCase();

    return jdbi.withHandle(handle -> {
      PermissionDao dao = handle.attach(PermissionDao.class);

      Boolean explicit = resolveFromList(dao.getPlayerPermissions(uuid.toString()), node);
      if (explicit != null) {
        return explicit;
      }

      List<Integer> groupIds = dao.getPlayerGroupIds(uuid.toString());
      if (groupIds.isEmpty()) {
        groupIds = dao.getDefaultGroupIds();
      }

      groupIds.sort((a, b) -> Integer.compare(dao.getGroupPriority(b), dao.getGroupPriority(a)));

      Set<Integer> visited = new HashSet<>();
      for (int gid : groupIds) {
        Boolean result = resolveGroupPermission(dao, gid, node, visited);
        if (result != null) {
          return result;
        }
      }
      return false;
    });
  }

  private Boolean resolveGroupPermission(PermissionDao dao, int groupId, String target,
      Set<Integer> visited) {
    if (!visited.add(groupId)) {
      return null;
    }

    Boolean result = resolveFromList(dao.getGroupPermissions(groupId), target);
    if (result != null) {
      return result;
    }

    for (int parentId : dao.getParentGroupIds(groupId)) {
      Boolean parentResult = resolveGroupPermission(dao, parentId, target, visited);
      if (parentResult != null) {
        return parentResult;
      }
    }
    return null;
  }

  private Boolean resolveFromList(List<String> permissions, String target) {
    if (permissions.contains("-" + target)) {
      return false;
    }
    if (permissions.contains(target)) {
      return true;
    }
    for (String p : permissions) {
      boolean negated = p.startsWith("-");
      String node = negated ? p.substring(1) : p;
      if (matchesWildcard(node, target)) {
        return !negated;
      }
    }
    return null;
  }

  private boolean matchesWildcard(String pattern, String target) {
    if (pattern.equals("*")) {
      return true;
    }
    if (pattern.endsWith(".*")) {
      String prefix = pattern.substring(0, pattern.length() - 2);
      return target.equals(prefix) || target.startsWith(prefix + ".");
    }
    return pattern.equals(target);
  }

  @Override
  public void addPlayerPermission(UUID uuid, String permission) {
    jdbi.useExtension(PermissionDao.class,
        dao -> dao.addPlayerPermission(uuid.toString(), permission.toLowerCase()));
  }

  @Override
  public void denyPlayerPermission(UUID uuid, String permission) {
    addPlayerPermission(uuid, "-" + permission.toLowerCase());
  }

  @Override
  public void removePlayerPermission(UUID uuid, String permission) {
    jdbi.useExtension(PermissionDao.class,
        dao -> dao.removePlayerPermission(uuid.toString(), permission.toLowerCase(),
            "-" + permission.toLowerCase()));
  }

  @Override
  public List<String> getPlayerPermissions(UUID uuid) {
    return jdbi.withExtension(PermissionDao.class,
        dao -> dao.getPlayerPermissions(uuid.toString()));
  }

  @Override
  public void addPlayerToGroup(UUID uuid, String groupName) {
    jdbi.useExtension(PermissionDao.class, dao ->
        dao.getGroupIdByName(groupName).ifPresent(id -> dao.addPlayerToGroup(uuid.toString(), id)));
  }

  @Override
  public void removePlayerFromGroup(UUID uuid, String groupName) {
    jdbi.useExtension(PermissionDao.class, dao ->
        dao.getGroupIdByName(groupName)
            .ifPresent(id -> dao.removePlayerFromGroup(uuid.toString(), id)));
  }

  @Override
  public List<String> getPlayerGroups(UUID uuid) {
    return jdbi.withExtension(PermissionDao.class, dao -> dao.getPlayerGroupNames(uuid.toString()));
  }

  @Override
  public void createGroup(String name, int priority) {
    jdbi.useExtension(PermissionDao.class, dao -> dao.createGroup(name.toLowerCase(), priority));
  }

  @Override
  public void deleteGroup(String name) {
    jdbi.useExtension(PermissionDao.class, dao -> dao.deleteGroup(name.toLowerCase()));
  }

  @Override
  public Optional<PermissionGroup> getGroup(String name) {
    return jdbi.withExtension(PermissionDao.class, dao ->
        dao.getGroup(name.toLowerCase()).map(record -> {
          record.setManager(this);
          return record;
        }));
  }

  @Override
  public List<PermissionGroup> allGroups() {
    return jdbi.withExtension(PermissionDao.class, dao -> {
      List<PermissionGroupRecord> records = dao.allGroups();
      records.forEach(r -> r.setManager(this));
      return new ArrayList<>(records);
    });
  }

  @Override
  public void addGroupPermission(String groupName, String permission) {
    jdbi.useExtension(PermissionDao.class, dao ->
        dao.getGroupIdByName(groupName)
            .ifPresent(id -> dao.addGroupPermission(id, permission.toLowerCase())));
  }

  @Override
  public void removeGroupPermission(String groupName, String permission) {
    jdbi.useExtension(PermissionDao.class, dao ->
        dao.getGroupIdByName(groupName)
            .ifPresent(id -> dao.removeGroupPermission(id, permission.toLowerCase())));
  }

  @Override
  public void addGroupInheritance(String groupName, String parentName) {
    jdbi.useExtension(PermissionDao.class, dao -> {
      Optional<Integer> gid = dao.getGroupIdByName(groupName);
      Optional<Integer> pid = dao.getGroupIdByName(parentName);
      if (gid.isPresent() && pid.isPresent()) {
        dao.addGroupInheritance(gid.get(), pid.get());
      }
    });
  }

  @Override
  public void removeGroupInheritance(String groupName, String parentName) {
    jdbi.useExtension(PermissionDao.class, dao -> {
      Optional<Integer> gid = dao.getGroupIdByName(groupName);
      Optional<Integer> pid = dao.getGroupIdByName(parentName);
      if (gid.isPresent() && pid.isPresent()) {
        dao.removeGroupInheritance(gid.get(), pid.get());
      }
    });
  }

  @Override
  public void setGroupPrefix(String groupName, String prefix) {
    jdbi.useExtension(PermissionDao.class,
        dao -> dao.setGroupPrefix(groupName.toLowerCase(), prefix));
  }

  @Override
  public void setGroupSuffix(String groupName, String suffix) {
    jdbi.useExtension(PermissionDao.class,
        dao -> dao.setGroupSuffix(groupName.toLowerCase(), suffix));
  }

  @Override
  public void setGroupDefault(String groupName, boolean isDefault) {
    jdbi.useHandle(handle -> {
      PermissionDao dao = handle.attach(PermissionDao.class);
      if (isDefault) {
        dao.clearDefaults();
      }
      dao.setGroupDefault(groupName.toLowerCase(), isDefault ? 1 : 0);
    });
  }
}