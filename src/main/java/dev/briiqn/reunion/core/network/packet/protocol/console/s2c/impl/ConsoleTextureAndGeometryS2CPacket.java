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

package dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.util.StringUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 160, supports = {39, 78})
public final class ConsoleTextureAndGeometryS2CPacket extends ConsoleS2CPacket {

  private final String textureName;
  private final int dwSkinID;
  private final byte[] textureData;       // null or empty = request
  private final int animOverride;
  private final byte[] boxData;           // raw box bytes, length must be boxCount*34
  private final int boxCount;

  public ConsoleTextureAndGeometryS2CPacket() {
    this("", 0, new byte[0], 0, new byte[0], 0);
  }

  // if we send 0 bytes in our payload the client assumes we are REQUESTING for their skin.
  public ConsoleTextureAndGeometryS2CPacket(String textureName, int dwSkinID) {
    this.textureName = textureName;
    this.dwSkinID = dwSkinID;
    this.textureData = new byte[0];
    this.animOverride = 0;
    this.boxData = new byte[0];
    this.boxCount = 0;
  }

  // our reply (this is what we send to other players)
  public ConsoleTextureAndGeometryS2CPacket(String textureName, int dwSkinID,
      byte[] textureData,
      int animOverride,
      byte[] boxData, int boxCount) {
    this.textureName = textureName;
    this.dwSkinID = dwSkinID;
    this.textureData = textureData != null ? textureData : new byte[0];
    this.animOverride = animOverride;
    this.boxData = boxData != null ? boxData : new byte[0];
    this.boxCount = boxCount;
  }

  @Override
  public void write(ByteBuf buf) {
    StringUtil.writeConsoleModifiedUtf8(buf, textureName);
    buf.writeInt(dwSkinID);
    buf.writeShort(textureData.length);
    buf.writeBytes(textureData);
    buf.writeInt(animOverride);
    buf.writeShort(boxCount);
    buf.writeBytes(boxData);
  }
}