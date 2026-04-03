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

import dev.briiqn.reunion.api.event.player.PreJoinEvent;
import dev.briiqn.reunion.core.manager.ChannelManager;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaCustomPayloadC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaHeldItemChangeC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x01, supports = {47})
public final class JavaJoinGameS2CPacket extends JavaS2CPacket {

  private static final byte[] REGISTER_PAYLOAD =
      String.join("\0", ChannelManager.getRegisteredChannels()).getBytes(StandardCharsets.UTF_8);

  private int entityId;
  private short gamemode;
  private byte dimension;
  private short difficulty;
  private String levelType;

  public JavaJoinGameS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = buf.readInt();
    gamemode = buf.readUnsignedByte();
    dimension = buf.readByte();
    difficulty = buf.readUnsignedByte();
    buf.readUnsignedByte(); // Max players (ignored)
    levelType = StringUtil.readJavaString(buf);
    if (buf.isReadable()) {
      buf.readBoolean(); // Reduced Debug Info
    }
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    var apiPlayer = new ConsoleSessionPlayerAdapter(cs);

    if (cs.getPendingJavaSession() == session) {
      if (cs.getJavaSession() != null) {
        cs.getJavaSession().close(true);
      }
      cs.cleanupForWorldChange();
      cs.getPlayerManager().clear();
      cs.setJavaSession(session);
      cs.setPendingJavaSession(null);
      cs.setPreviousServerName(cs.getCurrentServer());
      cs.setCurrentServer(cs.getPendingServerName());
      cs.setPendingServerName(null);
      cs.setFallingBack(false);
    } else if (cs.isLoggedIn()) {
      cs.cleanupForWorldChange();
    }

    PreJoinEvent preJoin = PluginEventHooks.firePreJoin(apiPlayer);
    if (!preJoin.isAllowed()) {
      PacketManager.sendToConsole(cs, new ConsoleDisconnectS2CPacket());
      cs.getConsoleChannel().close();
      return;
    }

    ByteBuf brandBuf = Unpooled.buffer();
    StringUtil.writeJavaString(brandBuf, "Reunion/" + cs.getClientVersion());
    PacketManager.sendToJava(session, new JavaCustomPayloadC2SPacket("MC|Brand", brandBuf));

    PacketManager.sendToJava(session,
        new JavaCustomPayloadC2SPacket("REGISTER",
            Unpooled.wrappedBuffer(REGISTER_PAYLOAD)));

    ChannelManager.onJoin(cs);

    cs.onJoinGame(entityId, gamemode, dimension, difficulty, levelType);

    cs.setUsingItem(false);
    cs.setNeedsReblock(false);
    int currentSlot = cs.getHeldItemSlot();
    PacketManager.sendToJava(session, new JavaHeldItemChangeC2SPacket((short) currentSlot));

    PluginEventHooks.fireJoin(apiPlayer);
  }
}