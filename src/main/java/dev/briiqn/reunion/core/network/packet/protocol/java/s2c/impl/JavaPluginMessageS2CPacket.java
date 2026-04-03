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
import static dev.briiqn.reunion.core.util.game.ItemUtil.readJavaItem;
import static dev.briiqn.reunion.core.util.game.ItemUtil.writeConsoleItemAuto;

import dev.briiqn.reunion.core.manager.ChannelManager;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleCustomPayloadS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x3F, supports = {47})
public final class JavaPluginMessageS2CPacket extends JavaS2CPacket {

  private String channel;
  private byte[] raw;

  public JavaPluginMessageS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    channel = readJavaString(buf);
    raw = new byte[buf.readableBytes()];
    buf.readBytes(raw);
  }

  @Override
  public void handle(JavaSession session) {
    ConsoleSession cs = session.getConsoleSession();
    if (session.getConsoleSession().getServer().getConfig().getNetwork().isNetworkMode()) {
      if (ChannelManager.isSupported(channel)) {
        ChannelManager.handleIncoming(cs, channel, raw);
        return;
      }
    }
    if ("MC|TrList".equals(channel)) {
      handleTradeList(cs);
      return;
    }

    boolean forward = PluginEventHooks.firePluginMessageFromServer(
        new ConsoleSessionPlayerAdapter(cs), channel, raw);
    if (forward) {
      PacketManager.sendToConsole(cs, new ConsoleCustomPayloadS2CPacket(channel, raw));
    }
  }

  private void handleTradeList(ConsoleSession cs) {
    ByteBuf payload = Unpooled.wrappedBuffer(raw);
    ByteBuf out = Unpooled.buffer();
    try {
      out.writeInt(payload.readInt());
      int count = payload.readByte() & 0xFF;
      out.writeByte(count);
      for (int i = 0; i < count; i++) {
        writeConsoleItemAuto(out, readJavaItem(payload));
        writeConsoleItemAuto(out, readJavaItem(payload));
        boolean hasSecond = payload.readBoolean();
        out.writeBoolean(hasSecond);
        if (hasSecond) {
          writeConsoleItemAuto(out, readJavaItem(payload));
        }
        out.writeBoolean(payload.readBoolean());
        out.writeInt(payload.readInt());
        out.writeInt(payload.readInt());
      }
      byte[] bytes = new byte[out.readableBytes()];
      out.readBytes(bytes);
      PacketManager.sendToConsole(cs, new ConsoleCustomPayloadS2CPacket("MC|TrList", bytes));
    } finally {
      out.release();
      payload.release();
    }
  }
}