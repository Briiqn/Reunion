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

package dev.briiqn.reunion.core.network.packet.protocol.java.s2c.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleComplexItemDataS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x34, supports = {47})
public final class JavaMapDataS2CPacket extends JavaS2CPacket {

  private int mapId;
  private byte scale;
  private Icon[] icons;
  private int columns;
  private int rows;
  private int xOffset;
  private int zOffset;
  private byte[] mapData;

  public JavaMapDataS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    mapId = VarIntUtil.read(buf);
    scale = buf.readByte();
    int iconCount = VarIntUtil.read(buf);
    icons = new Icon[iconCount];
    for (int i = 0; i < iconCount; i++) {
      byte dirType = buf.readByte();
      byte x = buf.readByte();
      byte z = buf.readByte();
      icons[i] = new Icon(dirType, x, z);
    }

    columns = buf.readUnsignedByte();
    if (columns > 0) {
      rows = buf.readUnsignedByte();
      xOffset = buf.readUnsignedByte();
      zOffset = buf.readUnsignedByte();
      int dataLength = VarIntUtil.read(buf);
      mapData = new byte[dataLength];
      buf.readBytes(mapData);
    }
  }

  @Override
  public void handle(JavaSession session) {
    PacketManager.sendToConsole(session.getConsoleSession(), new ConsoleComplexItemDataS2CPacket(
        (short) 358, (short) mapId, new byte[]{2, scale}
    ));

    if (icons.length > 0) {
      byte[] decPayload = new byte[icons.length * 8 + 1];
      decPayload[0] = 1; // HEADER_DECORATIONS
      for (int i = 0; i < icons.length; i++) {
        Icon icon = icons[i];
        byte img = (byte) ((icon.dirType >> 4) & 0xF);
        byte rot = (byte) (icon.dirType & 0xF);

        int offset = i * 8;
        decPayload[offset + 1] = img;
        decPayload[offset + 2] = icon.x;
        decPayload[offset + 3] = icon.z;

        // entityId (4 bytes)
        decPayload[offset + 4] = 0;
        decPayload[offset + 5] = 0;
        decPayload[offset + 6] = 0;
        decPayload[offset + 7] = (byte) 0x80; // 0x80 = visible flag

        decPayload[offset + 8] = rot;
      }
      PacketManager.sendToConsole(session.getConsoleSession(), new ConsoleComplexItemDataS2CPacket(
          (short) 358, (short) mapId, decPayload
      ));
    }

    // java sends a 2D patch. console expects 1 column per packet.
    if (columns > 0 && mapData != null) {
      for (int c = 0; c < columns; c++) {
        byte[] colPayload = new byte[rows + 3];
        colPayload[0] = 0; // HEADER_COLOURS
        colPayload[1] = (byte) (xOffset + c);
        colPayload[2] = (byte) zOffset; // yOffset

        for (int r = 0; r < rows; r++) {
          int index = c + r * columns;
          if (index < mapData.length) {
            colPayload[3 + r] = mapData[index];
          }
        }

        PacketManager.sendToConsole(session.getConsoleSession(),
            new ConsoleComplexItemDataS2CPacket(
                (short) 358, (short) mapId, colPayload
            ));
      }
    }
  }

  private record Icon(byte dirType, byte x, byte z) {

  }
}