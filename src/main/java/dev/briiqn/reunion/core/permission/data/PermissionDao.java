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

package dev.briiqn.reunion.core.permission.data;

import java.util.List;
import java.util.Optional;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface PermissionDao {

  @SqlQuery("SELECT * FROM permission_groups WHERE name = :name")
  @RegisterConstructorMapper(PermissionGroupRecord.class)
  Optional<PermissionGroupRecord> getGroup(@Bind("name") String name);

  @SqlQuery("SELECT * FROM permission_groups ORDER BY priority DESC")
  @RegisterConstructorMapper(PermissionGroupRecord.class)
  List<PermissionGroupRecord> allGroups();

  @SqlUpdate("INSERT OR IGNORE INTO permission_groups(name, priority) VALUES(:name, :priority)")
  void createGroup(@Bind("name") String name, @Bind("priority") int priority);

  @SqlUpdate("DELETE FROM permission_groups WHERE name = :name")
  void deleteGroup(@Bind("name") String name);

  @SqlUpdate("UPDATE permission_groups SET prefix = :prefix WHERE name = :name")
  void setGroupPrefix(@Bind("name") String name, @Bind("prefix") String prefix);

  @SqlUpdate("UPDATE permission_groups SET suffix = :suffix WHERE name = :name")
  void setGroupSuffix(@Bind("name") String name, @Bind("suffix") String suffix);

  @SqlUpdate("UPDATE permission_groups SET default_group = 0")
  void clearDefaults();

  @SqlUpdate("UPDATE permission_groups SET default_group = :isDefault WHERE name = :name")
  void setGroupDefault(@Bind("name") String name, @Bind("isDefault") int isDefault);

  @SqlQuery("SELECT id FROM permission_groups WHERE name = :name")
  Optional<Integer> getGroupIdByName(@Bind("name") String name);

  @SqlQuery("SELECT priority FROM permission_groups WHERE id = :id")
  int getGroupPriority(@Bind("id") int id);

  @SqlQuery("SELECT permission FROM player_permissions WHERE uuid = :uuid")
  List<String> getPlayerPermissions(@Bind("uuid") String uuid);

  @SqlUpdate("INSERT OR IGNORE INTO player_permissions(uuid, permission) VALUES(:uuid, :perm)")
  void addPlayerPermission(@Bind("uuid") String uuid, @Bind("perm") String perm);

  @SqlUpdate("DELETE FROM player_permissions WHERE uuid = :uuid AND (permission = :perm OR permission = :negated)")
  void removePlayerPermission(@Bind("uuid") String uuid, @Bind("perm") String perm,
      @Bind("negated") String negated);

  @SqlQuery("SELECT permission FROM group_permissions WHERE group_id = :groupId")
  List<String> getGroupPermissions(@Bind("groupId") int groupId);

  @SqlUpdate("INSERT OR IGNORE INTO group_permissions(group_id, permission) VALUES(:groupId, :perm)")
  void addGroupPermission(@Bind("groupId") int groupId, @Bind("perm") String perm);

  @SqlUpdate("DELETE FROM group_permissions WHERE group_id = :groupId AND permission = :perm")
  void removeGroupPermission(@Bind("groupId") int groupId, @Bind("perm") String perm);


  @SqlQuery("SELECT pg.group_id FROM player_groups pg WHERE pg.uuid = :uuid")
  List<Integer> getPlayerGroupIds(@Bind("uuid") String uuid);

  @SqlQuery("SELECT name FROM permission_groups g INNER JOIN player_groups pg ON g.id = pg.group_id WHERE pg.uuid = :uuid ORDER BY g.priority DESC")
  List<String> getPlayerGroupNames(@Bind("uuid") String uuid);

  @SqlUpdate("INSERT OR IGNORE INTO player_groups(uuid, group_id) VALUES(:uuid, :groupId)")
  void addPlayerToGroup(@Bind("uuid") String uuid, @Bind("groupId") int groupId);

  @SqlUpdate("DELETE FROM player_groups WHERE uuid = :uuid AND group_id = :groupId")
  void removePlayerFromGroup(@Bind("uuid") String uuid, @Bind("groupId") int groupId);

  @SqlQuery("SELECT parent_id FROM group_inheritance WHERE group_id = :groupId")
  List<Integer> getParentGroupIds(@Bind("groupId") int groupId);

  @SqlUpdate("INSERT OR IGNORE INTO group_inheritance(group_id, parent_id) VALUES(:groupId, :parentId)")
  void addGroupInheritance(@Bind("groupId") int groupId, @Bind("parentId") int parentId);

  @SqlUpdate("DELETE FROM group_inheritance WHERE group_id = :groupId AND parent_id = :parentId")
  void removeGroupInheritance(@Bind("groupId") int groupId, @Bind("parentId") int parentId);

  @SqlQuery("SELECT id FROM permission_groups WHERE default_group = 1")
  List<Integer> getDefaultGroupIds();

  @SqlQuery("SELECT g.name FROM permission_groups g INNER JOIN group_inheritance gi ON g.id = gi.parent_id WHERE gi.group_id = :groupId")
  List<String> getInheritedGroupNames(@Bind("groupId") int groupId);
}