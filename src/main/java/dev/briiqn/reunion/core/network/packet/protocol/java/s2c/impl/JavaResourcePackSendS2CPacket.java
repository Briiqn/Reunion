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

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaResourcePackStatusC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.StringUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x48, supports = {47})
public final class JavaResourcePackSendS2CPacket extends JavaS2CPacket {

  private String url;
  private String hash;

  public JavaResourcePackSendS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    url = StringUtil.readJavaString(buf);
    hash = StringUtil.readJavaString(buf);
  }

  @Override
  public void handle(JavaSession session) {
    if (session.getConsoleSession().getServer().getConfig().getGameplay()
        .isAutoAcceptResourcePacks()) {
      PacketManager.sendToJava(session, new JavaResourcePackStatusC2SPacket(hash, 3)); // ACCEPTED
      PacketManager.sendToJava(session,
          new JavaResourcePackStatusC2SPacket(hash, 0)); // SUCCESSFULLY_LOADED
    }
  }
}