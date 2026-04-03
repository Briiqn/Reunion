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

import static dev.briiqn.reunion.core.util.StringUtil.readConsoleUtf;

import dev.briiqn.reunion.api.event.player.PlayerChatEvent;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaChatMessageC2SPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 3, supports = {39, 78})
public final class ConsoleChatC2SPacket extends ConsoleC2SPacket {

  private String message;

  public ConsoleChatC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    buf.readShort();
    int packedCounts = buf.readUnsignedShort();
    int stringCount = (packedCounts >> 4) & 0xF;
    int intCount = packedCounts & 0xF;

    if (stringCount > 0) {
      message = readConsoleUtf(buf);
      for (int i = 1; i < stringCount; i++) {
        readConsoleUtf(buf);
      }
    } else {
      message = "";
    }

    for (int i = 0; i < intCount; i++) {
      buf.readInt();
    }
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    String trimmed = message.trim();

    if (trimmed.startsWith("/")) {
      if (session.getServer().getCommandManager().dispatch(session, trimmed)) {
        return;
      }
    }

    PlayerChatEvent event = PluginEventHooks.fireChat(
        new ConsoleSessionPlayerAdapter(session), message);

    if (event.isCancelled()) {
      return;
    }

    PacketManager.sendToJava(session.getJavaSession(),
        new JavaChatMessageC2SPacket(event.message()));
  }
}