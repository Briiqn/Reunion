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
import dev.briiqn.reunion.core.network.packet.data.enums.TitleAction;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x45, supports = {47})
public final class JavaTitleS2CPacket extends JavaS2CPacket {

  private TitleAction action;
  private String json;
  private int fadeIn, stay, fadeOut;

  public JavaTitleS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    action = TitleAction.fromId(VarIntUtil.read(buf));
    if (action == TitleAction.TITLE || action == TitleAction.SUBTITLE) {
      json = readJavaString(buf);
    } else if (action == TitleAction.TIMES) {
      fadeIn = buf.readInt();
      stay = buf.readInt();
      fadeOut = buf.readInt();
    }
  }

  @Override
  public void handle(JavaSession session) {
    if ((action == TitleAction.TITLE || action == TitleAction.SUBTITLE) && json != null) {
      try {
        String parsedText = parseJsonText(JSON.parseObject(json));
        if (!parsedText.isEmpty()) {
          session.getConsoleSession().queueOrSendChat(parsedText);
        }
      } catch (Exception ignored) {
      }
    }
  }

  private String parseJsonText(JSONObject object) {
    if (object == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();

    if (object.containsKey("text")) {
      sb.append(object.getString("text"));
    }
    if (object.containsKey("extra")) {
      JSONArray extra = object.getJSONArray("extra");
      for (int i = 0; i < extra.size(); i++) {
        Object extraItem = extra.get(i);
        if (extraItem instanceof JSONObject) {
          sb.append(parseJsonText((JSONObject) extraItem));
        } else if (extraItem instanceof String) {
          sb.append(extraItem);
        }
      }
    }
    return sb.toString();
  }
}