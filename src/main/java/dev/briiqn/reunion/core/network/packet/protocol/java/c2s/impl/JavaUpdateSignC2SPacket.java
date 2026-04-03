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

package dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl;

import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.game.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_C2S, id = 0x12, supports = {47})
public final class JavaUpdateSignC2SPacket extends JavaC2SPacket {

  private int x, y, z;
  private String[] lines;

  public JavaUpdateSignC2SPacket() {
  }

  public JavaUpdateSignC2SPacket(int x, int y, int z, String[] lines) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.lines = lines;
  }

  @Override
  public void write(ByteBuf buf) {
    BufferUtil.writePosition(buf, x, y, z);
    for (int i = 0; i < 4; i++) {
      String text = (i < lines.length && lines[i] != null) ? lines[i] : "";
      JSONObject json = new JSONObject();
      json.put("text", text);
      StringUtil.writeJavaString(buf, json.toString());
    }
  }
}