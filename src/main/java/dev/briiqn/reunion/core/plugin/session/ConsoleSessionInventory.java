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

package dev.briiqn.reunion.core.plugin.session;

import dev.briiqn.reunion.api.player.ItemStack;
import dev.briiqn.reunion.api.player.PlayerInventory;
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.container.InventoryTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Adapts the proxy's {@link InventoryTracker} to the plugin-facing {@link PlayerInventory}
 * interface.
 */
final class ConsoleSessionInventory implements PlayerInventory {

  private final ConsoleSession session;

  ConsoleSessionInventory(ConsoleSession session) {
    this.session = session;
  }

  private static ItemStack adapt(ItemInstance item) {
    if (item == null || item.id() == -1) {
      return ItemStack.EMPTY;
    }
    return new ItemStack(item.id(), item.count(), item.damage(), item.nbt());
  }

  private InventoryTracker tracker() {
    return session.getInventoryTracker();
  }

  @Override
  public ItemStack slot(int slot) {
    if (slot < 0 || slot >= TOTAL_SLOTS) {
      throw new IndexOutOfBoundsException("Slot out of range: " + slot);
    }
    ItemInstance raw = tracker().getPlayerInventoryArray()[slot];
    return adapt(raw);
  }

  @Override
  public List<ItemStack> allSlots() {
    List<ItemInstance> raw = tracker().getItemsAsList();
    List<ItemStack> result = new ArrayList<>(TOTAL_SLOTS);
    for (int i = 0; i < TOTAL_SLOTS; i++) {
      result.add(i < raw.size() ? adapt(raw.get(i)) : ItemStack.EMPTY);
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public List<ItemStack> hotbar() {
    List<ItemStack> bar = new ArrayList<>(9);
    for (int i = HOTBAR_START; i <= HOTBAR_END; i++) {
      bar.add(slot(i));
    }
    return Collections.unmodifiableList(bar);
  }

  @Override
  public ItemStack heldItem() {
    return slot(HOTBAR_START + heldSlot());
  }

  @Override
  public int heldSlot() {
    return session.getHeldItemSlot();
  }

  @Override
  public ItemStack[] armour() {
    return new ItemStack[]{slot(5), slot(6), slot(7), slot(8)};
  }

  @Override
  public Optional<Integer> findItem(short itemId) {
    for (int i = 9; i <= HOTBAR_END; i++) {
      ItemStack s = slot(i);
      if (!s.isEmpty() && s.id() == itemId) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Integer> findItem(short itemId, short damage) {
    for (int i = 9; i <= HOTBAR_END; i++) {
      ItemStack s = slot(i);
      if (!s.isEmpty() && s.id() == itemId && s.damage() == damage) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }

  @Override
  public int countItem(short itemId) {
    int total = 0;
    for (int i = 9; i <= HOTBAR_END; i++) {
      ItemStack s = slot(i);
      if (!s.isEmpty() && s.id() == itemId) {
        total += (s.count() & 0xFF);
      }
    }
    return total;
  }
}
