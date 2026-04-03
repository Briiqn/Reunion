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

package dev.briiqn.reunion.core.network.packet.protocol.console.c2s.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.StringUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;


@Log4j2
@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 160, supports = {39, 78})
public final class ConsoleTextureAndGeometryC2SPacket extends ConsoleC2SPacket {

  private String textureName;
  private int dwSkinID;
  private byte[] textureData;
  private int animOverride;
  private byte[] boxData;
  private int boxCount;

  public ConsoleTextureAndGeometryC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    textureName = StringUtil.readConsoleModifiedUtf8(buf);
    dwSkinID = buf.readInt();
    int len = buf.readUnsignedShort();
    if (len > 0) {
      textureData = new byte[len];
      buf.readBytes(textureData);
    } else {
      textureData = null;
    }
    animOverride = buf.readInt();
    boxCount = buf.readUnsignedShort();
    if (boxCount > 0) {
      int boxBytes = boxCount * 34;
      boxData = new byte[boxBytes];
      buf.readBytes(boxData);
    } else {
      boxData = null;
    }
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    if (textureName == null || textureName.isEmpty()) {
      return;
    }

    if (textureData == null) {
      ConsoleTextureC2SPacket.handleRequest(session, textureName, dwSkinID);
    } else {
      ConsoleTextureC2SPacket.handleDelivery(
          session, textureName, textureData, dwSkinID, boxData, boxCount, animOverride);
    }
  }
}