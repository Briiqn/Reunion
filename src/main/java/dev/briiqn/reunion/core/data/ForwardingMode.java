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

package dev.briiqn.reunion.core.data;

public enum ForwardingMode {

  /**
   * No forwarding. The proxy is not trusted and player IPs/UUIDs come directly from the connecting
   * client.
   */
  NONE,

  /**
   * Legacy BungeeCord forwarding (ip-forward: true in BungeeCord config.yml). Player IP and UUID
   * are passed in the handshake hostname field. No shared secret  should only be used on a network
   * that is fully firewalled.
   */
  BUNGEECORD,

  /**
   * BungeeGuard forwarding  BungeeCord forwarding with an HMAC token injected into the login
   * payload to authenticate the proxy. Requires {@code bungee-guard-token} to be set.
   */
  BUNGEEGUARD,

  /**
   * Velocity modern forwarding (forwarding-secret in velocity.toml). Uses a cryptographic
   * HMAC-SHA256 signature; the most secure option. Requires {@code velocity-secret} to be set.
   */
  VELOCITY;

  /**
   * Case-insensitive parse with a fallback default.
   */
  public static ForwardingMode fromString(String value, ForwardingMode fallback) {
    if (value == null) {
      return fallback;
    }
    try {
      return ForwardingMode.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return fallback;
    }
  }
}