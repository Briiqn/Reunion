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

package dev.briiqn.reunion.api.world;

/**
 * An entity currently tracked in a player's world view.
 *
 * <p>The proxy tracks entities based on spawn/destroy packets received from the
 * Java backend. Position and rotation are updated as move packets arrive.
 */
public interface TrackedEntity {

  /**
   * The Java Edition entity ID assigned by the backend server. This maps to the console-side entity
   * ID via the entity manager.
   */
  int javaEntityId();

  /**
   * The console-side entity ID visible to the LCE client.
   */
  int consoleEntityId();

  /**
   * The entity type ID in the Java 1.8 protocol.
   */
  int typeId();

  /**
   * The entity's category as classified by the proxy.
   */
  EntityCategory category();


  /**
   * The entity's last known Java-side X position (fixed-point, divided by 32.0 gives the real
   * coordinate).
   */
  int rawX();

  /**
   * The entity's last known Java-side Y position.
   */
  int rawY();

  /**
   * The entity's last known Java-side Z position.
   */
  int rawZ();

  /**
   * The entity's last known yaw as a raw byte.
   */
  byte yaw();

  /**
   * The entity's last known pitch as a raw byte.
   */
  byte pitch();

  /**
   * The entity's real X coordinate (rawX divided by 32.0).
   */
  default double x() {
    return rawX() / 32.0;
  }

  /**
   * The entity's real Y coordinate.
   */
  default double y() {
    return rawY() / 32.0;
  }

  /**
   * The entity's real Z coordinate.
   */
  default double z() {
    return rawZ() / 32.0;
  }

  /**
   * The entity's position as a {@link Location} in the default overworld dimension.
   */
  default Location location() {
    return Location.of(x(), y(), z());
  }


  /**
   * Entity categories used internally by the proxy.
   */
  enum EntityCategory {
    MOB,
    OBJECT,
    PLAYER,
    EXPERIENCE_ORB,
    PAINTING,
    GLOBAL_ENTITY,
    UNKNOWN
  }
}
