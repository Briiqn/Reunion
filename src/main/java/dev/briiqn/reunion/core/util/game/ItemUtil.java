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

package dev.briiqn.reunion.core.util.game;

import static dev.briiqn.reunion.core.util.VarIntUtil.read;

import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.registry.impl.ItemRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class ItemUtil {

  private ItemUtil() {
  }

  public static boolean isUsable(ItemInstance item) {
    if (item == null || item.id() == -1) {
      return false;
    }
    short id = item.id();
    return id == 261 || // Bow
        id == 373 || // Potion
        id == 325 || // Milk Bucket
        isSword(item) ||
        id == 260 || id == 282 || id == 297 || id == 319 || id == 320 || id == 322 ||
        id == 349 || id == 350 || id == 357 || id == 360 || id == 363 || id == 364 ||
        id == 365 || id == 366 || id == 367 || id == 375 || id == 391 || id == 392 ||
        id == 393 || id == 394 || id == 396 || id == 400 || id == 411 || id == 412 ||
        id == 413 || id == 423 || id == 424; // Vanilla 1.8 Food
  }

  public static boolean isSword(ItemInstance item) {
    if (item == null || item.id() == -1) {
      return false;
    }
    short id = item.id();
    return id == 268 || id == 272 || id == 267 || id == 283 || id == 276; // All Swords
  }

  public static ItemInstance readConsoleItem(ByteBuf in) {
    short id = in.readShort();
    if (id == -1) {
      return null;
    }

    byte count = in.readByte();
    short damage = in.readShort();
    short nbtLen = in.readShort();
    byte[] nbt = null;

    if (nbtLen > 0) {
      byte[] rawBlob = new byte[nbtLen];
      in.readBytes(rawBlob);
      nbt = trimNbtData(rawBlob);
    }

    return new ItemInstance(id, count, damage, nbt);
  }

  public static ItemInstance readJavaItem(ByteBuf in) {
    short id = in.readShort();
    if (id == -1) {
      return null;
    }

    byte count = in.readByte();
    short damage = in.readShort();

    int readerIndex = in.readerIndex();
    int nbtType = in.readByte() & 0xFF;

    byte[] nbt;
    if (nbtType == 0) {
      nbt = null;
    } else {
      in.readerIndex(readerIndex);
      nbt = NbtUtil.readRaw(in);
    }

    return applyMeta(id, count, damage, nbt);
  }

  public static void writeConsoleItem(ByteBuf out, ItemInstance item) {
    if (item == null || item.id() == -1) {
      out.writeShort(-1);
      return;
    }

    out.writeShort(item.id());
    out.writeByte(item.count());
    out.writeShort(item.damage());
    writeNbtConsole(out, item.nbt());
  }

  public static void writeJavaItem(ByteBuf out, ItemInstance item) {
    if (item == null || item.id() <= 0) {
      out.writeShort(-1);
      return;
    }

    out.writeShort(item.id());
    out.writeByte(item.count());
    out.writeShort(item.damage());

    if (item.nbt() != null && item.nbt().length > 0) {
      out.writeBytes(item.nbt());
    } else {
      out.writeByte(0);
    }
  }

  public static void writeConsoleItemNamed(ByteBuf out, ItemInstance item, String name) {
    if (item == null || item.id() == -1) {
      out.writeShort(-1);
      return;
    }

    out.writeShort(item.id());
    out.writeByte(item.count());
    out.writeShort(item.damage());
    writeNbtConsole(out, name != null ? NbtUtil.injectDisplayName(item.nbt(), name) : item.nbt());
  }

  public static void writeConsoleItemAuto(ByteBuf out, ItemInstance item) {
    if (item == null || item.id() == -1) {
      out.writeShort(-1);
      return;
    }

    int id = item.id();
    int meta = Short.toUnsignedInt(item.damage());

    if (id > 0 && id < 256) {
      BlockRegistry blocks = BlockRegistry.getInstance();
      if (blocks.isUnsupported(id)) {
        int[] remapped = blocks.remapBlock(id, meta);
        writeConsoleItem(out,
            new ItemInstance((short) remapped[0], item.count(), (short) remapped[1], item.nbt()));
      } else {
        writeConsoleItem(out, item);
      }
    } else if (id >= 256) {
      ItemRegistry items = ItemRegistry.getInstance();
      if (items.isUnsupported(id)) {
        int fallbackId = items.remap(id, meta);
        writeConsoleItem(out,
            new ItemInstance((short) fallbackId, item.count(), (short) 0, item.nbt()));
      } else {
        writeConsoleItem(out, item);
      }
    } else {
      writeConsoleItem(out, item);
    }
  }

  public static byte readMetadataFlags(ByteBuf in) {
    byte flags = 0;
    while (true) {
      int item = in.readUnsignedByte();
      if (item == 127) {
        break;
      }

      int type = item >> 5;
      int index = item & 0x1F;

      switch (type) {
        case 0 -> {
          byte val = in.readByte();
          if (index == 0) {
            flags = val;
          }
        }
        case 1 -> in.readShort();
        case 2 -> in.readInt();
        case 3 -> in.readFloat();
        case 4 -> in.skipBytes(read(in));
        case 5 -> readConsoleItem(in);
        case 6 -> {
          in.readInt();
          in.readInt();
          in.readInt();
        }
        case 7 -> {
          in.readFloat();
          in.readFloat();
          in.readFloat();
        }
      }
    }
    return flags;
  }

  private static ItemInstance applyMeta(short id, byte count, short damage, byte[] nbt) {
    int meta = Short.toUnsignedInt(damage);

    boolean unsupported;
    String displayName;

    if (id > 0 && id < 256) {
      BlockRegistry blocks = BlockRegistry.getInstance();
      unsupported = blocks.isUnsupported(id);
      displayName = unsupported ? blocks.getDisplayName(id, meta) : null;
    } else if (id >= 256) {
      ItemRegistry items = ItemRegistry.getInstance();
      unsupported = items.isUnsupported(id);
      displayName = unsupported ? items.getDisplayName(id, meta) : null;
    } else {
      return new ItemInstance(id, count, damage, nbt);
    }

    if (!unsupported || displayName == null) {
      return new ItemInstance(id, count, damage, nbt);
    }

    if (!NbtUtil.hasDisplayName(nbt)) {
      nbt = NbtUtil.injectDisplayName(nbt, displayName);
    }

    return new ItemInstance(id, count, damage, nbt);
  }

  private static void writeNbtConsole(ByteBuf out, byte[] nbt) {
    if (nbt != null && nbt.length > 0) {
      if (nbt.length > 32767) {
        // If NBT bytes exceed short max val, LCE will rollover into the negatives causing some issues.
        out.writeShort(-1);
      } else {
        out.writeShort(nbt.length);
        out.writeBytes(nbt);
      }
    } else {
      out.writeShort(-1);
    }
  }

  private static byte[] trimNbtData(byte[] input) {
    if (input == null || input.length == 0) {
      return input;
    }
    ByteBuf buf = Unpooled.wrappedBuffer(input);
    try {
      byte[] actual = NbtUtil.readRaw(buf);
      return actual != null ? actual : input;
    } catch (Exception e) {
      return input;
    } finally {
      buf.release();
    }
  }
}