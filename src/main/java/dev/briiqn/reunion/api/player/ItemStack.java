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


/**
 * Represents an item stack in a player's inventory.
 *
 * <p>Immutable value object. All "modification" methods return new instances.
 * Use {@link Builder} to construct instances.
 */
public record ItemStack(short id, byte count, short damage, byte[] nbt) {

  /**
   * An empty/air item slot.
   */
  public static final ItemStack EMPTY = new ItemStack((short) -1, (byte) 0, (short) 0, null);

  public static Builder builder(short id) {
    return new Builder(id);
  }

  /**
   * Returns {@code true} if this slot is air/empty.
   */
  public boolean isEmpty() {
    return id == -1 || count <= 0;
  }

  /**
   * Returns the item ID. {@code -1} indicates an empty slot (air).
   */
  @Override
  public short id() {
    return id;
  }

  /**
   * The stack size.
   */
  @Override
  public byte count() {
    return count;
  }

  /**
   * The item's damage/metadata value. For tools this is durability; for blocks with variants (e.g.
   * wool) it's the variant.
   */
  @Override
  public short damage() {
    return damage;
  }

  /**
   * The raw NBT tag bytes, or {@code null} if this item has no NBT. This is the Java Edition
   * compound NBT blob.
   */
  @Override
  public byte[] nbt() {
    return nbt;
  }

  /**
   * Returns a new ItemStack with the given stack size.
   */
  public ItemStack withCount(byte count) {
    return new ItemStack(id, count, damage, nbt);
  }

  /**
   * Returns a new ItemStack with the given damage value.
   */
  public ItemStack withDamage(short damage) {
    return new ItemStack(id, count, damage, nbt);
  }


  public static final class Builder {

    private final short id;
    private byte count = 1;
    private short damage = 0;
    private byte[] nbt = null;

    private Builder(short id) {
      this.id = id;
    }

    public Builder count(byte count) {
      this.count = count;
      return this;
    }

    public Builder count(int count) {
      this.count = (byte) count;
      return this;
    }

    public Builder damage(short damage) {
      this.damage = damage;
      return this;
    }

    public Builder damage(int damage) {
      this.damage = (short) damage;
      return this;
    }

    public Builder nbt(byte[] nbt) {
      this.nbt = nbt;
      return this;
    }

    public ItemStack build() {
      return new ItemStack(id, count, damage, nbt);
    }
  }
}
