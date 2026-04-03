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

import static dev.briiqn.reunion.core.util.StringUtil.readJavaString;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x02, supports = {47})
public final class JavaChatMessageS2CPacket extends JavaS2CPacket {

  private String json;
  private byte position;

  public JavaChatMessageS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    this.json = readJavaString(buf);
    this.position = buf.readByte(); // 0: chat, 1: system, 2: actionbar
  }

  @Override
  public void handle(JavaSession session) {
    try {
      JSONObject object = JSON.parseObject(json);
      String result = parseJsonText(object);

      if (result != null && !result.isEmpty()) {
        sendSplitMessage(session, result);
      }
    } catch (Exception ignored) {
    }
  }

  private String parseJsonText(JSONObject object) {
    if (object == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();

    if (object.containsKey("sender")) {
      sb.append(object.getString("sender")).append(": ");
    }
    if (object.containsKey("content")) {
      sb.append(object.getString("content"));
    }

    if (object.containsKey("text")) {
      sb.append(object.getString("text"));
    }

    if (object.containsKey("extra")) {
      JSONArray extra = object.getJSONArray("extra");
      if (extra != null) {
        for (int i = 0; i < extra.size(); i++) {
          Object extraItem = extra.get(i);
          if (extraItem instanceof JSONObject) {
            sb.append(parseJsonText((JSONObject) extraItem));
          } else if (extraItem instanceof String) {
            sb.append(extraItem);
          }
        }
      }
    }
    return sb.toString();
  }

  private void sendSplitMessage(JavaSession session, String message) {
    final int MAX_LEN = 100;

    if (message.length() <= MAX_LEN) {
      session.getConsoleSession().queueOrSendChat(message);
      return;
    }

    String[] tokens = message.split(" ");
    StringBuilder currentLine = new StringBuilder();

    for (String token : tokens) {
      if (currentLine.length() + token.length() + 1 > MAX_LEN) {
        session.getConsoleSession().queueOrSendChat(currentLine.toString());
        currentLine = new StringBuilder();
      }

      if (!currentLine.isEmpty()) {
        currentLine.append(" ");
      }
      currentLine.append(token);
    }

    if (!currentLine.isEmpty()) {
      session.getConsoleSession().queueOrSendChat(currentLine.toString());
    }
  }
}