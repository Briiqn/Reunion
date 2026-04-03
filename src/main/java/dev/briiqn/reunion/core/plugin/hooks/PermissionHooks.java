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

package dev.briiqn.reunion.core.plugin.hooks;

import dev.briiqn.reunion.api.permission.PermissionManager;
import java.util.UUID;

/**
 * Static bridge between the API-layer command sources and the live {@link PermissionManager}.
 *
 * <p>Initialised once by {@code ReunionProxyImpl} after constructing
 * {@code PermissionManagerImpl}. After that, any API class (e.g. {@code PlayerCommandSource}) can
 * call {@link #hasPermission(UUID, String)} without a direct dependency on the core module.
 *
 * <p>If the permission manager has not yet been initialised, all checks return {@code true}
 * so that early-boot code is never blocked.
 */
public final class PermissionHooks {

  private static PermissionManager permissionManager;

  private PermissionHooks() {
  }

  /**
   * Called once during server start-up to register the active manager.
   *
   * @param pm the permission manager implementation
   */
  public static void init(PermissionManager pm) {
    permissionManager = pm;
  }

  /**
   * Returns {@code true} if the player with the given UUID has the specified permission. Returns
   * {@code true} unconditionally if the manager has not been initialised yet.
   *
   * @param uuid       the player's UUID
   * @param permission the permission node to test
   */
  public static boolean hasPermission(UUID uuid, String permission) {
    if (permissionManager == null) {
      return true;
    }
    return permissionManager.hasPermission(uuid, permission);
  }

  /**
   * Returns the active {@link PermissionManager}, or {@code null} if not yet initialised.
   */
  public static PermissionManager get() {
    return permissionManager;
  }
}