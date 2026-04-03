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

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the set of currently online players.
 */
public interface PlayerManager {

  /**
   * Returns an immutable snapshot of all currently online players.
   */
  Collection<? extends Player> onlinePlayers();

  /**
   * Looks up a player by username (case-insensitive).
   *
   * @param username the player's username
   * @return an Optional with the player, or empty if not online
   */
  Optional<Player> player(String username);

  /**
   * Looks up a player by UUID.
   *
   * @param uuid the player's UUID
   * @return an Optional with the player, or empty if not online
   */
  Optional<Player> player(UUID uuid);

  /**
   * Returns the number of currently online players.
   */
  int count();
}
