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

package dev.briiqn.reunion.core.util.container;

import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

@Getter
public class InventoryTracker {

  private static final int PLAYER_SLOTS = 46;
  private static final int WINDOW_SLOTS = 128;

  private final ItemInstance[] playerInventoryArray = new ItemInstance[PLAYER_SLOTS];
  private final ItemInstance[] openWindowArray = new ItemInstance[WINDOW_SLOTS];

  @Getter
  @Setter
  private ItemInstance cursorItem = null;

  private byte windowId = 0;
  @Setter
  private int windowSize = 45;

  public void setWindowId(byte windowId) {
    this.windowId = windowId;
    if (windowId == 0) {
      this.windowSize = 45;
      Arrays.fill(openWindowArray, null);
    }
  }

  public void reset() {
    this.windowId = 0;
    this.windowSize = 45;
    this.cursorItem = null;
    Arrays.fill(playerInventoryArray, null);
    Arrays.fill(openWindowArray, null);
  }

  public void setItem(int packetWindowId, int slot, ItemInstance item) {
    ItemInstance cursorItem1 = (item == null || item.id() == -1) ? null : item;
    if (packetWindowId == -1 || slot == -1) {
      this.cursorItem = cursorItem1;
      return;
    }

    if (packetWindowId == 0) {
      if (slot >= 0 && slot < PLAYER_SLOTS) {
        playerInventoryArray[slot] = cursorItem1;
      }
    } else {
      if (slot >= 0 && slot < WINDOW_SLOTS) {
        openWindowArray[slot] = cursorItem1;
      }
    }
  }

  public Map<Integer, ItemInstance> getActiveSlots() {
    return windowId == 0
        ? new ArrayBackedMap(playerInventoryArray, PLAYER_SLOTS)
        : new ArrayBackedMap(openWindowArray, WINDOW_SLOTS);
  }

  public List<ItemInstance> getItemsAsList() {
    ItemInstance[] active = (windowId == 0) ? playerInventoryArray : openWindowArray;
    int size = Math.max(windowSize, active.length);
    ItemInstance empty = new ItemInstance((short) -1, (byte) 0, (short) 0);
    List<ItemInstance> list = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      ItemInstance item = (i < active.length) ? active[i] : null;
      list.add(item != null ? item : empty);
    }
    return list;
  }

  private static final class ArrayBackedMap extends AbstractMap<Integer, ItemInstance> {

    private final ItemInstance[] array;
    private final int capacity;

    ArrayBackedMap(ItemInstance[] array, int capacity) {
      this.array = array;
      this.capacity = capacity;
    }

    @Override
    public ItemInstance get(Object key) {
      if (!(key instanceof Integer i)) {
        return null;
      }
      return (i >= 0 && i < capacity) ? array[i] : null;
    }

    @Override
    public ItemInstance put(Integer key, ItemInstance value) {
      if (key < 0 || key >= capacity) {
        return null;
      }
      ItemInstance old = array[key];
      array[key] = (value == null || value.id() == -1) ? null : value;
      return old;
    }

    @Override
    public ItemInstance remove(Object key) {
      if (!(key instanceof Integer i)) {
        return null;
      }
      if (i < 0 || i >= capacity) {
        return null;
      }
      ItemInstance old = array[i];
      array[i] = null;
      return old;
    }

    @Override
    public void clear() {
      Arrays.fill(array, 0, capacity, null);
    }

    @Override
    public boolean containsKey(Object key) {
      if (!(key instanceof Integer i)) {
        return false;
      }
      return i >= 0 && i < capacity && array[i] != null;
    }

    @Override
    public int size() {
      int count = 0;
      for (int i = 0; i < capacity; i++) {
        if (array[i] != null) {
          count++;
        }
      }
      return count;
    }

    @Override
    public @NonNull Set<Map.Entry<Integer, ItemInstance>> entrySet() {
      return new ArrayEntrySet();
    }

    private final class ArrayEntrySet extends AbstractSet<Map.Entry<Integer, ItemInstance>> {

      @Override
      public int size() {
        return ArrayBackedMap.this.size();
      }

      @Override
      public @NonNull Iterator<Map.Entry<Integer, ItemInstance>> iterator() {
        return new Iterator<>() {
          private int cursor = advanceTo(0);

          private int advanceTo(int from) {
            while (from < capacity && array[from] == null) {
              from++;
            }
            return from;
          }

          @Override
          public boolean hasNext() {
            return cursor < capacity;
          }

          @Override
          public Map.Entry<Integer, ItemInstance> next() {
            int slot = cursor;
            cursor = advanceTo(slot + 1);
            return new Map.Entry<>() {
              @Override
              public Integer getKey() {
                return slot;
              }

              @Override
              public ItemInstance getValue() {
                return array[slot];
              }

              @Override
              public ItemInstance setValue(ItemInstance v) {
                ItemInstance old = array[slot];
                array[slot] = (v == null || v.id() == -1) ? null : v;
                return old;
              }
            };
          }
        };
      }
    }
  }
}