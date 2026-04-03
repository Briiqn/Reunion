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

import dev.briiqn.reunion.api.permission.PermissionGroup;
import dev.briiqn.reunion.core.permission.PermissionManagerImpl;
import java.util.List;
import lombok.Setter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class PermissionGroupRecord implements PermissionGroup {

  private final int id;
  private final String name;
  private final int priority;
  private final String prefix;
  private final String suffix;
  private final boolean isDefault;
  @Setter
  private PermissionManagerImpl manager;

  public PermissionGroupRecord(
      @ColumnName("id") int id,
      @ColumnName("name") String name,
      @ColumnName("priority") int priority,
      @ColumnName("prefix") String prefix,
      @ColumnName("suffix") String suffix,
      @ColumnName("default_group") int default_group) {
    this.id = id;
    this.name = name;
    this.priority = priority;
    this.prefix = prefix;
    this.suffix = suffix;
    this.isDefault = default_group == 1;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int priority() {
    return priority;
  }

  @Override
  public String prefix() {
    return prefix;
  }

  @Override
  public String suffix() {
    return suffix;
  }

  @Override
  public boolean isDefault() {
    return isDefault;
  }

  @Override
  public List<String> permissions() {
    return manager.jdbi.withExtension(PermissionDao.class, dao -> dao.getGroupPermissions(id));
  }

  @Override
  public List<String> inheritedGroups() {
    return manager.jdbi.withExtension(PermissionDao.class, dao -> dao.getInheritedGroupNames(id));
  }
}