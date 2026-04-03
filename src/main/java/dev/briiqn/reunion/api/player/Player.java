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

package dev.briiqn.reunion.api.player;

import dev.briiqn.reunion.api.packet.PacketPipeline;
import dev.briiqn.reunion.api.world.BlockBuffer;
import dev.briiqn.reunion.api.world.BlockPosition;
import dev.briiqn.reunion.api.world.Location;
import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;


/**
 * Represents a player connected to the Reunion proxy.
 *
 * <p>A player exists from when their login packet is received
 * ({@link dev.briiqn.reunion.api.event.player.LoginEvent}) until they disconnect
 * ({@link dev.briiqn.reunion.api.event.player.PlayerDisconnectEvent}).
 *
 * <p>Since Reunion is a proxy bridging LCE Console ↔ Java Edition, a "player"
 * encompasses both the console-side connection and the Java-side backend session.
 */
public interface Player {


  /**
   * The player's username, as sent in the login packet.
   */
  String username();

  /**
   * The player's unique identifier. For online-mode sessions this is the Mojang UUID; for offline
   * sessions it is a UUID derived from the username.
   */
  UUID uuid();

  /**
   * The remote address of the console client's TCP connection.
   */
  InetSocketAddress remoteAddress();

  /**
   * The protocol version reported by the console client. Known values: {@code 39} (older LCE) and
   * {@code 78} (newer LCE).
   */
  int clientVersion();


  /**
   * Returns {@code true} if the player has fully joined and their session is active.
   */
  boolean isOnline();

  /**
   * Returns {@code true} if the player is currently connected to a Java backend server.
   */
  boolean isConnectedToJava();

  /**
   * The name of the backend server this player is connected to, if any.
   */
  Optional<String> currentServer();

  /**
   * The player's current game mode.
   */
  GameMode gameMode();

  /**
   * The player's current position in the world (from the last known location packet).
   */
  Location location();

  /**
   * Convenience method  returns the player's current (x, y, z) position as a JOML
   * {@link dev.briiqn.reunion.core.util.math.vector.Vec3d}. Backed by {@link #location()}.
   */
  default Vec3d position() {
    return location().position();
  }

  /**
   * Convenience method  returns the player's current (yaw, pitch) rotation as a JOML {@link Vec2f}.
   * Backed by {@link #location()}.
   */
  default Vec2f rotation() {
    return location().rotation();
  }

  /**
   * The number of server ticks this player has been alive since joining. Incremented once per
   * movement tick (each time {@code flushTickActions} runs).
   */
  long ticksAlive();

  /**
   * The player's current health (0.0 – 20.0).
   */
  float health();

  /**
   * The player's current food level (0 – 20).
   */
  int foodLevel();

  /**
   * Returns {@code true} if the player is currently sneaking.
   */
  boolean isSneaking();

  /**
   * Returns {@code true} if the player is currently sprinting.
   */
  boolean isSprinting();

  /**
   * Returns {@code true} if the player is currently riding an entity.
   */
  boolean isRiding();

  /**
   * The entity ID of the vehicle this player is riding, or {@code -1} if not riding.
   */
  int ridingEntityId();


  /**
   * The player's current inventory.
   */
  PlayerInventory inventory();


  /**
   * Sends a chat message to this player's console client.
   *
   * @param message the message text (LCE supports basic formatting codes via §)
   */
  void sendMessage(String message);

  /**
   * Sends a plugin message on the given channel to the console client.
   *
   * @param channel the channel identifier
   * @param data    the message payload
   */
  void sendPluginMessage(String channel, byte[] data);

  /**
   * Sends a plugin message on the given channel to the Java backend server.
   *
   * @param channel the channel identifier
   * @param data    the message payload
   */
  void sendPluginMessageToServer(String channel, byte[] data);


  /**
   * Disconnects this player from the proxy with the given reason.
   *
   * @param reason the disconnect message shown to the player
   */
  void disconnect(String reason);

  /**
   * Disconnects the player from the current Java backend server without closing the console
   * client's connection.
   */
  void disconnectFromBackend();

  /**
   * Transfers this player to a different backend server.
   *
   * @param serverName the name of the target server (as defined in the proxy config)
   * @throws IllegalArgumentException if no server with that name is configured
   */
  void connectTo(String serverName);

  /**
   * Teleports the player to the given location. This sends a position-and-look packet to both the
   * console client and the Java backend.
   *
   * @param location the target location
   */
  void teleport(Location location);


  /**
   * Sends a single block change to the player's client. This method automatically handles
   * coordinate remapping for the infinite world hack.
   *
   * @param pos      the block's absolute world position
   * @param blockId  the new block ID
   * @param metadata the new block metadata
   */
  void sendBlockChange(BlockPosition pos, int blockId, int metadata);

  /**
   * Sends a region of blocks to the player's client from a {@link BlockBuffer}. The Reunion core
   * handles all necessary encoding (RLE, compression) and coordinate remapping.
   *
   * @param origin the world position of the buffer's (0,0,0) corner
   * @param buffer the block buffer to send
   */
  void sendBlockBuffer(BlockPosition origin, BlockBuffer buffer);


  /**
   * The raw packet pipeline for this player. Use to intercept, inject, or drop packets at the Netty
   * layer.
   */
  PacketPipeline packetPipeline();

  /**
   * Returns the internal small session ID used by the LCE protocol. This is a byte (1–127) assigned
   * when the console client connects. Most plugins will not need this.
   */
  byte sessionSmallId();

  /**
   * Returns the Java entity ID assigned by the backend server, or {@code -1} if the player has not
   * yet received their join-game packet.
   */
  int javaEntityId();

  /**
   * Stores a named metadata value on this player for the lifetime of their session. Useful for
   * plugins that need to attach state to a player without subclassing.
   *
   * @param key   the metadata key
   * @param value the value to store
   */
  void setMetadata(String key, Object value);

  /**
   * Retrieves a previously stored metadata value.
   *
   * @param key the metadata key
   * @return an Optional containing the value, or empty if not set
   */
  Optional<Object> getMetadata(String key);

  /**
   * Removes a metadata entry.
   *
   * @param key the metadata key to remove
   */
  void removeMetadata(String key);
}