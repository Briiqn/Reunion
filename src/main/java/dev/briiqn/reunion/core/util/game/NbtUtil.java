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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Map;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public final class NbtUtil {

  private NbtUtil() {
  }

  public static byte[] readRaw(ByteBuf in) {
    int startIndex = in.readerIndex();

    try {
      skipNbt(in);
    } catch (Exception e) {
      in.readerIndex(startIndex);
      return null;
    }

    int endIndex = in.readerIndex();
    int length = endIndex - startIndex;

    if (length <= 0) {
      return null;
    }

    byte[] rawNbt = new byte[length];
    in.getBytes(startIndex, rawNbt);

    return sanitize(rawNbt);
  }

  private static byte[] sanitize(byte[] rawNbt) {
    if (rawNbt == null || rawNbt.length == 0) {
      return rawNbt;
    }
    try {
      ByteBuf buf = Unpooled.wrappedBuffer(rawNbt);
      Map.Entry<String, CompoundBinaryTag> named;
      try (ByteBufInputStream is = new ByteBufInputStream(buf)) {
        named = BinaryTagIO.reader().readNamed((java.io.InputStream) is);
      }

      CompoundBinaryTag cleaned = cleanCompound(named.getValue());
      return writeNamedCompound(named.getKey(), cleaned);
    } catch (Exception e) {
      return null;
    }
  }

  private static byte[] writeNamedCompound(String name, CompoundBinaryTag tag) throws IOException {
    ByteBuf outBuf = Unpooled.buffer();
    try (ByteBufOutputStream bbos = new ByteBufOutputStream(outBuf)) {
      BinaryTagIO.writer().writeNamed(Map.entry(name, tag), (java.io.OutputStream) bbos);
      byte[] bytes = new byte[outBuf.readableBytes()];
      outBuf.readBytes(bytes);
      return bytes;
    } finally {
      outBuf.release();
    }
  }

  private static CompoundBinaryTag cleanCompound(CompoundBinaryTag compound) {
    CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
    for (Map.Entry<String, ? extends BinaryTag> entry : compound) {
      String key = entry.getKey();
      BinaryTag tag = entry.getValue();

      if (shouldRemove(tag)) {
        continue;
      }

      if (tag instanceof CompoundBinaryTag) {
        builder.put(key, cleanCompound((CompoundBinaryTag) tag));
      } else if (tag instanceof ListBinaryTag) {
        builder.put(key, cleanList((ListBinaryTag) tag));
      } else {
        builder.put(key, tag);
      }
    }
    return builder.build();
  }

  private static ListBinaryTag cleanList(ListBinaryTag list) {
    ListBinaryTag.Builder<BinaryTag> builder = ListBinaryTag.builder();
    for (BinaryTag tag : list) {
      if (shouldRemove(tag)) {
        continue;
      }

      if (tag instanceof CompoundBinaryTag) {
        builder.add(cleanCompound((CompoundBinaryTag) tag));
      } else if (tag instanceof ListBinaryTag) {
        builder.add((BinaryTag) cleanList((ListBinaryTag) tag));
      } else {
        builder.add(tag);
      }
    }
    return builder.build();
  }

  private static boolean shouldRemove(BinaryTag tag) {
    return switch (tag) {
      case null -> true;
      case LongArrayBinaryTag longs -> true;
      case ByteArrayBinaryTag bytes -> bytes.value().length == 0;
      case IntArrayBinaryTag integers -> integers.value().length == 0;
      default -> false;
    };
  }

  public static boolean hasDisplayName(byte[] rawNbt) {
    if (rawNbt == null || rawNbt.length == 0) {
      return false;
    }
    try {
      ByteBuf buf = Unpooled.wrappedBuffer(rawNbt);
      try (ByteBufInputStream is = new ByteBufInputStream(buf)) {
        CompoundBinaryTag root = BinaryTagIO.reader().read((java.io.InputStream) is);
        CompoundBinaryTag display = root.getCompound("display");
        return display != CompoundBinaryTag.empty()
            && !display.getString("Name").isEmpty();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public static byte[] injectDisplayName(byte[] rawNbt, String name) {
    String rootName = "";
    CompoundBinaryTag root;

    if (rawNbt == null || rawNbt.length == 0) {
      root = CompoundBinaryTag.empty();
    } else {
      try {
        ByteBuf buf = Unpooled.wrappedBuffer(rawNbt);
        try (ByteBufInputStream is = new ByteBufInputStream(buf)) {
          Map.Entry<String, CompoundBinaryTag> named = BinaryTagIO.reader()
              .readNamed((java.io.InputStream) is);
          rootName = named.getKey();
          root = named.getValue();
        }
      } catch (Exception e) {
        root = CompoundBinaryTag.empty();
      }
    }

    CompoundBinaryTag display = root.getCompound("display");
    CompoundBinaryTag newDisplay = copyCompoundWith(display, "Name",
        StringBinaryTag.stringBinaryTag(name));

    CompoundBinaryTag newRoot = copyCompoundWith(root, "display", newDisplay);

    try {
      return writeNamedCompound(rootName, newRoot);
    } catch (IOException e) {
      return rawNbt;
    }
  }

  private static CompoundBinaryTag copyCompoundWith(CompoundBinaryTag source, String key,
      BinaryTag value) {
    CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
    for (Map.Entry<String, ? extends BinaryTag> entry : source) {
      builder.put(entry.getKey(), entry.getValue());
    }
    builder.put(key, value);
    return builder.build();
  }


  private static void skipNbt(ByteBuf buf) {
    byte type = buf.readByte();
    if (type == 0) {
      return;
    }
    skipString(buf); // Root name
    skipPayload(buf, type);
  }

  private static void skipPayload(ByteBuf buf, byte type) {
    switch (type) {
      case 0:
        break;
      case 1:
        buf.skipBytes(1);
        break;
      case 2:
        buf.skipBytes(2);
        break;
      case 3:
        buf.skipBytes(4);
        break;
      case 4:
        buf.skipBytes(8);
        break;
      case 5:
        buf.skipBytes(4);
        break;
      case 6:
        buf.skipBytes(8);
        break;
      case 7:
        buf.skipBytes(buf.readInt());
        break;
      case 8:
        skipString(buf);
        break;
      case 9:
        byte listType = buf.readByte();
        int listLen = buf.readInt();
        if (listLen > 0 && listType > 0) {
          for (int i = 0; i < listLen; i++) {
            skipPayload(buf, listType);
          }
        }
        break;
      case 10:
        while (true) {
          byte childType = buf.readByte();
          if (childType == 0) {
            break; // End tag
          }
          skipString(buf); // Child name
          skipPayload(buf, childType);
        }
        break;
      case 11:
        buf.skipBytes(buf.readInt() * 4);
        break;
      case 12:
        buf.skipBytes(buf.readInt() * 8);
        break;
      default:
        throw new RuntimeException("Unknown NBT tag type: " + type);
    }
  }

  private static void skipString(ByteBuf buf) {
    int len = buf.readUnsignedShort();
    buf.skipBytes(len);
  }
}