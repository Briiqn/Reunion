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

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleRespawnS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaClientStatusC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x07, supports = {47})
public final class JavaRespawnS2CPacket extends JavaS2CPacket {

  private int dimension;
  private short difficulty, gamemode;
  private String levelType;

  public JavaRespawnS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    dimension = buf.readInt();
    difficulty = buf.readUnsignedByte();
    gamemode = buf.readUnsignedByte();
    levelType = readJavaString(buf);
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    cs.cleanupForWorldChange();
    cs.setDimension(dimension);
    cs.setGamemode(gamemode);
    cs.setDifficulty(difficulty);
    cs.setLevelType(levelType != null ? levelType : "DEFAULT");
    PacketManager.sendToJava(session, new JavaClientStatusC2SPacket(0));
    PacketManager.sendToConsole(cs, new ConsoleRespawnS2CPacket(
        dimension, gamemode, cs.getLevelType(), 0L, difficulty, cs.getSafePlayerId()));
  }
}