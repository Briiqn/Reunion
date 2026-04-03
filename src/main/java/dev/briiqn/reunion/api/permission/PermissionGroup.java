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

package dev.briiqn.reunion.api.permission;

import java.util.List;

/**
 * Represents a named permission group stored in the permission database.
 *
 * <p>Groups can inherit permissions from parent groups, have an optional prefix/suffix
 * for chat formatting, and carry a priority that determines evaluation order when a player belongs
 * to multiple groups (higher priority = evaluated first).
 *
 * <p>Negated permissions are stored with a {@code "-"} prefix
 * (e.g. {@code "-admin.kick"}) and override any positive grant from inherited groups.
 */
public interface PermissionGroup {

  /**
   * The unique, lowercase name of this group (e.g. {@code "admin"}, {@code "default"}).
   */
  String name();

  /**
   * The evaluation priority of this group. Higher values are resolved before lower ones when a
   * player belongs to multiple groups.
   */
  int priority();

  /**
   * The chat prefix associated with this group. May be an empty string.
   */
  String prefix();

  /**
   * The chat suffix associated with this group. May be an empty string.
   */
  String suffix();

  /**
   * Returns {@code true} if this group is the default group automatically applied to players who
   * have not been explicitly assigned to any other group.
   */
  boolean isDefault();

  /**
   * All permission nodes explicitly set on this group, including negated ones prefixed with
   * {@code "-"}.
   */
  List<String> permissions();

  /**
   * The names of groups whose permissions this group inherits. Inheritance is resolved recursively
   * and cycle-safe.
   */
  List<String> inheritedGroups();
}