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
 * Represents a Minecraft dimension.
 *
 * <p>Dimension IDs match the Java Edition 1.8 protocol values.
 */
public enum Dimension {

  NETHER(-1, "The Nether"),
  OVERWORLD(0, "Overworld"),
  END(1, "The End");

  private final int id;
  private final String displayName;

  Dimension(int id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  /**
   * Returns the Dimension for the given ID.
   *
   * @param id the dimension ID
   * @return the matching Dimension
   * @throws IllegalArgumentException if the ID is unknown
   */
  public static Dimension fromId(int id) {
    for (Dimension d : values()) {
      if (d.id == id) {
        return d;
      }
    }
    throw new IllegalArgumentException("Unknown dimension id: " + id);
  }

  /**
   * Returns the Dimension for the given ID, or {@link #OVERWORLD} as a fallback.
   */
  public static Dimension fromIdOrDefault(int id) {
    for (Dimension d : values()) {
      if (d.id == id) {
        return d;
      }
    }
    return OVERWORLD;
  }

  /**
   * The Java Edition dimension ID (-1, 0, or 1).
   */
  public int id() {
    return id;
  }

  /**
   * Human-readable display name.
   */
  public String displayName() {
    return displayName;
  }
}
