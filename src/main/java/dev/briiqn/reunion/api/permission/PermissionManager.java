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
import java.util.Optional;
import java.util.UUID;

/**
 * The SQLite-backed permission manager for Reunion.
 *
 * <p>Permissions are resolved in the following order (highest priority first):
 * <ol>
 *   <li>Explicit player permissions  negation always wins over a positive grant.</li>
 *   <li>Player-assigned groups, sorted by {@link PermissionGroup#priority()} descending.</li>
 *   <li>Inherited parent groups, resolved recursively and protected against cycles.</li>
 *   <li>The default group (applied when a player has no explicit group assignment).</li>
 * </ol>
 *
 * <p>Wildcard permissions ({@code "some.namespace.*"}) are supported. Granting
 * {@code "some.namespace.*"} covers every sub-node under that namespace. The special
 * node {@code "*"} grants all permissions.
 *
 * <p>Negated permissions use a {@code "-"} prefix (e.g. {@code "-admin.kick"}).
 * A negated player-level permission overrides any positive grant from a group.
 *
 * <p>The console always has every permission regardless of database state.
 *
 * <p>Access this manager via {@code proxy().permissionManager()} in your plugin.
 *
 * <p>Example usage:
 * <pre>{@code
 * PermissionManager perms = proxy().permissionManager();
 *
 * // Give a player the "moderator" group
 * perms.addPlayerToGroup(player.uuid(), "moderator");
 *
 * // Check a permission inside a command
 * if (!invocation.source().hasPermission("myplugin.reload")) {
 *     invocation.source().sendMessage("No permission.");
 *     return;
 * }
 * }</pre>
 */
public interface PermissionManager {

  // -------------------------------------------------------------------------
  // Permission check
  // -------------------------------------------------------------------------

  /**
   * Returns {@code true} if the player identified by {@code uuid} holds the given permission, after
   * resolving all group memberships and inheritance.
   *
   * @param uuid       the player's unique identifier
   * @param permission the permission node to test (e.g. {@code "myplugin.command.reload"})
   */
  boolean hasPermission(UUID uuid, String permission);

  // -------------------------------------------------------------------------
  // Player-level permissions
  // -------------------------------------------------------------------------

  /**
   * Explicitly grants a permission node to an individual player.
   *
   * @param uuid       the player's UUID
   * @param permission the permission node to grant
   */
  void addPlayerPermission(UUID uuid, String permission);

  /**
   * Explicitly denies a permission node for an individual player. Internally stored with a
   * {@code "-"} prefix and evaluated before any group grants.
   *
   * @param uuid       the player's UUID
   * @param permission the permission node to deny (without the {@code "-"} prefix)
   */
  void denyPlayerPermission(UUID uuid, String permission);

  /**
   * Removes an explicit player permission (either a grant or a denial). Both {@code "some.node"}
   * and {@code "-some.node"} are erased.
   *
   * @param uuid       the player's UUID
   * @param permission the permission node to remove
   */
  void removePlayerPermission(UUID uuid, String permission);

  /**
   * Returns all explicit permissions set on the player, including negated entries (prefixed
   * {@code "-"}).
   *
   * @param uuid the player's UUID
   */
  List<String> getPlayerPermissions(UUID uuid);

  // -------------------------------------------------------------------------
  // Player group membership
  // -------------------------------------------------------------------------

  /**
   * Adds the player to the named group.
   *
   * @param uuid      the player's UUID
   * @param groupName the group to join
   */
  void addPlayerToGroup(UUID uuid, String groupName);

  /**
   * Removes the player from the named group.
   *
   * @param uuid      the player's UUID
   * @param groupName the group to leave
   */
  void removePlayerFromGroup(UUID uuid, String groupName);

  /**
   * Returns the names of all groups the player is a member of.
   *
   * @param uuid the player's UUID
   */
  List<String> getPlayerGroups(UUID uuid);

  // -------------------------------------------------------------------------
  // Group management
  // -------------------------------------------------------------------------

  /**
   * Creates a new permission group. If a group with that name already exists, this call is a
   * no-op.
   *
   * @param name     the group name (will be lowercased)
   * @param priority the evaluation priority (higher = resolved first)
   */
  void createGroup(String name, int priority);

  /**
   * Deletes a group and removes all associated player memberships and group permissions.
   *
   * @param name the group name
   */
  void deleteGroup(String name);

  /**
   * Looks up a group by exact name (case-insensitive).
   *
   * @param name the group name
   * @return an {@link Optional} containing the group, or empty if not found
   */
  Optional<PermissionGroup> getGroup(String name);

  /**
   * Returns all registered groups sorted by priority descending.
   */
  List<PermissionGroup> allGroups();

  /**
   * Adds a permission node to a group. Prefix with {@code "-"} to add a negated (deny) entry.
   *
   * @param groupName  the group name
   * @param permission the permission node (may be negated with {@code "-"})
   */
  void addGroupPermission(String groupName, String permission);

  /**
   * Removes a permission node from a group.
   *
   * @param groupName  the group name
   * @param permission the permission node to remove
   */
  void removeGroupPermission(String groupName, String permission);

  /**
   * Makes {@code groupName} inherit all permissions from {@code parentName}. Circular inheritance
   * is safely short-circuited at resolution time.
   *
   * @param groupName  the child group
   * @param parentName the parent group to inherit from
   */
  void addGroupInheritance(String groupName, String parentName);

  /**
   * Removes the inheritance link between {@code groupName} and {@code parentName}.
   *
   * @param groupName  the child group
   * @param parentName the parent group
   */
  void removeGroupInheritance(String groupName, String parentName);

  /**
   * Sets the chat prefix for a group.
   *
   * @param groupName the group name
   * @param prefix    the new prefix (e.g. {@code "[Admin] "})
   */
  void setGroupPrefix(String groupName, String prefix);

  /**
   * Sets the chat suffix for a group.
   *
   * @param groupName the group name
   * @param suffix    the new suffix
   */
  void setGroupSuffix(String groupName, String suffix);

  /**
   * Marks or un-marks a group as the default group. When set to {@code true}, all previously marked
   * default groups are un-marked first so only one default exists.
   *
   * @param groupName the group name
   * @param isDefault {@code true} to make this the default group
   */
  void setGroupDefault(String groupName, boolean isDefault);
}