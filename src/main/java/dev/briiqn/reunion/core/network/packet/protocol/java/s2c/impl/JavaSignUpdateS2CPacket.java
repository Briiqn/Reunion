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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleSignUpdateS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.game.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x33, supports = {47})
public final class JavaSignUpdateS2CPacket extends JavaS2CPacket {

  private final String[] lines = new String[4];
  private int x, y, z;

  public JavaSignUpdateS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    long position = buf.readLong();
    this.x = BufferUtil.posX(position);
    this.y = BufferUtil.posY(position);
    this.z = BufferUtil.posZ(position);

    for (int i = 0; i < 4; i++) {
      lines[i] = StringUtil.readJavaString(buf);
    }
  }

  @Override
  public void handle(JavaSession session) {
    String[] cleanLines = new String[4];

    for (int i = 0; i < 4; i++) {
      try {
        if (lines[i].startsWith("{")) {
          JSONObject json = JSON.parseObject(lines[i]);
          cleanLines[i] = json.getString("text");
          if (cleanLines[i] == null) {
            cleanLines[i] = "";
          }
        } else if (lines[i].equals("null")) {
          cleanLines[i] = "";
        } else {
          cleanLines[i] = lines[i].replace("\"", "");
        }
      } catch (Exception e) {
        cleanLines[i] = "";
      }
    }

    var cs = session.getConsoleSession();
    int cx = cs.toConsoleX(x);
    int cz = cs.toConsoleZ(z);
    PacketManager.sendToConsole(cs,
        new ConsoleSignUpdateS2CPacket(cx, (short) y, cz, cleanLines));
  }
}