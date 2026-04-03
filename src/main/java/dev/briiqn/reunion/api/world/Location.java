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


import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;

/**
 * An immutable position + orientation in the game world.
 *
 * <p>Coordinates are expressed in Java Edition terms (before any proxy-side
 * world-offset translation is applied). Use {@link #toConsoleX(int)} / {@link #toConsoleZ(int)} to
 * get the console-side coordinates if needed.
 *
 * <p>JOML-backed accessors ({@link #position()}, {@link #rotation()}) are provided
 * for convenient math with the JOML library. The underlying scalar fields ({@link #x()},
 * {@link #y()}, {@link #z()}, {@link #yaw()}, {@link #pitch()}) remain the canonical storage  the
 * JOML objects are created on demand.
 */
public record Location(double x, double y, double z, float yaw, float pitch, Dimension dimension) {

  /**
   * Creates a location with default yaw/pitch (facing north) and the overworld dimension.
   */
  public static Location of(double x, double y, double z) {
    return new Location(x, y, z, 0f, 0f, Dimension.OVERWORLD);
  }

  /**
   * Creates a location in a specific dimension with default orientation.
   */
  public static Location of(double x, double y, double z, Dimension dimension) {
    return new Location(x, y, z, 0f, 0f, dimension);
  }

  /**
   * Creates a full location with orientation and dimension.
   */
  public static Location of(double x, double y, double z, float yaw, float pitch,
      Dimension dimension) {
    return new Location(x, y, z, yaw, pitch, dimension);
  }

  /**
   * Creates a location from a JOML {@link dev.briiqn.reunion.core.util.math.vector.Vec3d} position
   * with default orientation in the overworld.
   *
   * @param pos the (x, y, z) position vector
   */
  public static Location of(Vec3d pos) {
    return new Location(pos.x(), pos.y(), pos.z(), 0f, 0f, Dimension.OVERWORLD);
  }

  /**
   * Creates a location from a JOML {@link Vec3d} position in a specific dimension with default
   * orientation.
   *
   * @param pos       the (x, y, z) position vector
   * @param dimension the dimension
   */
  public static Location of(Vec3d pos, Dimension dimension) {
    return new Location(pos.x(), pos.y(), pos.z(), 0f, 0f, dimension);
  }

  /**
   * Creates a full location from JOML vectors.
   *
   * @param pos       the (x, y, z) position vector
   * @param rotation  the (yaw, pitch) rotation vector
   * @param dimension the dimension
   */
  public static Location of(Vec3d pos, Vec2f rotation, Dimension dimension) {
    return new Location(pos.x(), pos.y(), pos.z(), rotation.yaw(), rotation.pitch(), dimension);
  }

  /**
   * Returns the position as a JOML {@link Vec3d} containing (x, y, z).
   *
   * <p>A new vector is returned each call  mutating it does not affect this location.
   */
  public Vec3d position() {
    return new Vec3d(x, y, z);
  }

  /**
   * Returns the orientation as a JOML {@link Vec2f} containing (yaw, pitch).
   *
   * <p>A new vector is returned each call  mutating it does not affect this location.
   */
  public Vec2f rotation() {
    return new Vec2f(yaw, pitch);
  }

  /**
   * Returns the block X coordinate (floor of x).
   */
  public int blockX() {
    return (int) Math.floor(x);
  }

  /**
   * Returns the block Y coordinate (floor of y).
   */
  public int blockY() {
    return (int) Math.floor(y);
  }

  /**
   * Returns the block Z coordinate (floor of z).
   */
  public int blockZ() {
    return (int) Math.floor(z);
  }

  /**
   * Returns the chunk X coordinate this location falls in.
   */
  public int chunkX() {
    return blockX() >> 4;
  }

  /**
   * Returns the chunk Z coordinate this location falls in.
   */
  public int chunkZ() {
    return blockZ() >> 4;
  }

  /**
   * Returns this location as a {@link BlockPosition} (integer block coords).
   */
  public BlockPosition toBlockPosition() {
    return new BlockPosition(blockX(), blockY(), blockZ());
  }

  /**
   * Returns a new Location with modified coordinates.
   */
  public Location withPosition(double x, double y, double z) {
    return new Location(x, y, z, this.yaw, this.pitch, this.dimension);
  }

  /**
   * Returns a new Location with modified orientation.
   */
  public Location withLook(float yaw, float pitch) {
    return new Location(this.x, this.y, this.z, yaw, pitch, this.dimension);
  }

  /**
   * Returns a new Location in a different dimension.
   */
  public Location withDimension(Dimension dimension) {
    return new Location(this.x, this.y, this.z, this.yaw, this.pitch, dimension);
  }

  /**
   * Returns the Euclidean distance to another location. Does NOT check if the locations are in the
   * same dimension.
   */
  public double distanceTo(Location other) {
    double dx = this.x - other.x;
    double dy = this.y - other.y;
    double dz = this.z - other.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Returns the squared distance to another location (cheaper than distanceTo).
   */
  public double distanceSquaredTo(Location other) {
    double dx = this.x - other.x;
    double dy = this.y - other.y;
    double dz = this.z - other.z;
    return dx * dx + dy * dy + dz * dz;
  }

  @Override
  public String toString() {
    return String.format("Location{x=%.2f, y=%.2f, z=%.2f, yaw=%.1f, pitch=%.1f, dim=%s}",
        x, y, z, yaw, pitch, dimension);
  }
}