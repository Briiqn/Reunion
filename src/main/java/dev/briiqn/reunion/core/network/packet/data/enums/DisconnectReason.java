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

package dev.briiqn.reunion.core.network.packet.data.enums;

import lombok.Getter;

@Getter
public enum DisconnectReason {
  NONE(0),
  QUITTING(1),
  CLOSED(2),
  LOGIN_TOO_LONG(3),
  ILLEGAL_STANCE(4),
  ILLEGAL_POSITION(5),
  MOVED_TOO_QUICKLY(6),
  NO_FLYING(7),
  KICKED(8),
  TIMEOUT(9),
  OVERFLOW(10),
  END_OF_STREAM(11),
  SERVER_FULL(12),
  OUTDATED_SERVER(13),
  OUTDATED_CLIENT(14),
  UNEXPECTED_PACKET(15),
  CONNECTION_CREATION_FAILED(16),
  NO_MULTIPLAYER_PRIVILEGES_HOST(17),
  NO_MULTIPLAYER_PRIVILEGES_JOIN(18),
  NO_UGC_ALL_LOCAL(19),
  NO_UGC_SINGLE_LOCAL(20),
  CONTENT_RESTRICTED_ALL_LOCAL(21),
  CONTENT_RESTRICTED_SINGLE_LOCAL(22),
  NO_UGC_REMOTE(23),
  NO_FRIENDS_IN_GAME(24),
  BANNED(25),
  NOT_FRIENDS_WITH_HOST(26),
  NAT_MISMATCH(27),
  NETWORK_ERROR(28),
  EXITED_GAME(29);

  private final int id;

  DisconnectReason(int id) {
    this.id = id;
  }

  public static DisconnectReason fromId(int id) {
    for (DisconnectReason reason : values()) {
      if (reason.id == id) {
        return reason;
      }
    }
    return NONE;
  }
}