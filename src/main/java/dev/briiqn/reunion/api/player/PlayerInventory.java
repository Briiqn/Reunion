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

import java.util.List;
import java.util.Optional;

/**
 * Represents a player's inventory as tracked by the proxy.
 *
 * <p>Since Reunion is a proxy, inventory state is derived from the packets
 * observed passing through the pipeline. It may lag slightly behind the backend server's ground
 * truth, but is accurate for most purposes.
 *
 * <p>Slot layout (Java 1.8 protocol):
 * <pre>
 *   Slot  0       : Crafting output
 *   Slots 1–4     : Crafting grid
 *   Slots 5–8     : Armour (head, chest, legs, feet)
 *   Slots 9–35    : Main inventory (3 rows × 9 cols)
 *   Slots 36–44   : Hotbar
 * </pre>
 */
public interface PlayerInventory {

  /**
   * Total slot count of the player inventory window.
   */
  int TOTAL_SLOTS = 45;

  /**
   * First hotbar slot index.
   */
  int HOTBAR_START = 36;

  /**
   * Last hotbar slot index (inclusive).
   */
  int HOTBAR_END = 44;

  /**
   * Returns the item in the given slot, or {@link ItemStack#EMPTY} if the slot is air.
   *
   * @param slot the slot index (0–44)
   * @return the item stack in that slot
   * @throws IndexOutOfBoundsException if the slot is out of range
   */
  ItemStack slot(int slot);

  /**
   * Returns an unmodifiable list of all 46 item slots in order.
   */
  List<ItemStack> allSlots();

  /**
   * Returns the 9 hotbar slots (indices 36–44) as a list.
   */
  List<ItemStack> hotbar();

  /**
   * Returns the item in the currently selected hotbar slot.
   */
  ItemStack heldItem();

  /**
   * Returns the index of the currently selected hotbar slot (0–8, relative to hotbar start).
   */
  int heldSlot();

  /**
   * Returns the armour slots as an array of 4 items: {@code [helmet, chestplate, leggings, boots]}.
   * Empty slots are represented by {@link ItemStack#EMPTY}.
   */
  ItemStack[] armour();

  /**
   * Returns {@code true} if the given slot index is non-empty.
   *
   * @param slot the slot index to check
   */
  default boolean hasItem(int slot) {
    return !slot(slot).isEmpty();
  }

  /**
   * Finds the first slot in the main inventory or hotbar containing an item with the given ID.
   *
   * @param itemId the item ID to search for
   * @return an Optional containing the slot index, or empty if not found
   */
  Optional<Integer> findItem(short itemId);

  /**
   * Finds the first slot containing an item with the given ID and damage value.
   *
   * @param itemId the item ID
   * @param damage the damage/metadata value
   * @return an Optional containing the slot index, or empty if not found
   */
  Optional<Integer> findItem(short itemId, short damage);

  /**
   * Counts the total number of items with the given ID across all inventory slots.
   *
   * @param itemId the item ID to count
   * @return the total count
   */
  int countItem(short itemId);
}
