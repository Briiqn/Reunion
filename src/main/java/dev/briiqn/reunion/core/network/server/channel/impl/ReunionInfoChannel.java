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

package dev.briiqn.reunion.core.network.server.channel.impl;

import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaCustomPayloadC2SPacket;
import dev.briiqn.reunion.core.network.server.channel.PluginChannel;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.game.SkinUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ReunionInfoChannel extends PluginChannel {

  public ReunionInfoChannel() {
    super("reunion:");
  }

  public static void forward(ConsoleSession session) {
    if (session.getJavaSession() == null || !session.isLoggedIn()) {
      return;
    }

    ByteBuf buf = Unpooled.buffer();
    try {
      StringUtil.writeJavaString(buf, session.getPlayerName());
      buf.writeLong(session.getXuid());

      byte[] skin = null;
      if (session.getLceSkinName() != null) {
        byte[] rawSkin = session.getServer().getCachedLceTexture(session.getLceSkinName());
        if (rawSkin != null) {
          skin = SkinUtil.convertLceToJava(rawSkin);
        }
      }

      if (skin != null) {
        buf.writeInt(skin.length);
        buf.writeBytes(skin);
      } else {
        buf.writeInt(0);
      }

      byte[] cape = null;
      if (session.getLceCapeName() != null) {
        cape = session.getServer().getCachedLceTexture(session.getLceCapeName());
      }

      if (cape != null) {
        buf.writeInt(cape.length);
        buf.writeBytes(cape);
      } else {
        buf.writeInt(0);
      }

      PacketManager.sendToJava(session.getJavaSession(),
          new JavaCustomPayloadC2SPacket("reunion:", buf));
    } catch (Exception e) {
      log.error("Failed to forward player info for {}", session.getPlayerName(), e);
    } finally {
      buf.release();
    }
  }

  @Override
  public void onJoin(ConsoleSession session) {
    forward(session);
  }
}