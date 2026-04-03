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
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.InventoryType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleContainerCloseS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleContainerOpenS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.LanguageRegistry;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x2D, supports = {47})
public final class JavaOpenWindowS2CPacket extends JavaS2CPacket {

  private byte windowId;
  private String type;
  private String title;
  private int slots;
  private int entityId;

  @Override
  public void read(ByteBuf buf) {
    windowId = buf.readByte();
    type = readJavaString(buf);
    title = readJavaString(buf);
    slots = buf.readUnsignedByte();
    if (InventoryType.HORSE.getIdentifier().equals(type) && buf.isReadable(4)) {
      entityId = buf.readInt();
    }
  }

  @Override
  public void handle(JavaSession session) {
    ConsoleSession cs = session.getConsoleSession();
    byte currentWindow = cs.getInventoryTracker().getWindowId();
    if (currentWindow != 0 && currentWindow != windowId) {
      PacketManager.sendToConsole(cs, new ConsoleContainerCloseS2CPacket(currentWindow));
      cs.getInventoryTracker().setWindowId((byte) 0);
      cs.setWindowTransitioning(true);
      cs.queueTickAction(() -> {
        cs.setWindowTransitioning(false);
        this.handle(session);
      });
      return;
    }

    InventoryType invType = InventoryType.from(type);

    cs.setTradeWindowId((byte) -1);
    cs.setWindowType(windowId, invType.getId());

    cs.getInventoryTracker().setWindowId(windowId);
    cs.getInventoryTracker().setWindowSize(slots);

    int consoleEntityId = 0;
    if (invType == InventoryType.HORSE) {
      consoleEntityId = cs.getEntityManager().map(entityId);
    }

    String finalTitle = title == null ? "" : title;

    if (title != null) {
      if (title.startsWith("{")) {
        try {
          JSONObject json = JSON.parseObject(title);
          if (json.containsKey("text")) {
            finalTitle = json.getString("text");
          } else if (json.containsKey("translate")) {
            String key = json.getString("translate");
            finalTitle = LanguageRegistry.getInstance().translate(key);
          }
        } catch (Exception ignored) {
        }
      } else if (title.startsWith("\"") && title.endsWith("\"")) {
        finalTitle = title.substring(1, title.length() - 1);
      }
    }

    PacketManager.sendToConsole(cs,
        new ConsoleContainerOpenS2CPacket(windowId, invType.getId(), (short) slots, finalTitle,
            consoleEntityId));
  }
}