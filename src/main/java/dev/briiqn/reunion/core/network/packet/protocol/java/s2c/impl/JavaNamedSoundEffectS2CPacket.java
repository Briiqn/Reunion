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
import static dev.briiqn.reunion.core.util.game.SoundUtil.getConsoleSoundId;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleLevelSoundS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.math.vector.SoundVec3;
import io.netty.buffer.ByteBuf;


@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x29, supports = {47})
public final class JavaNamedSoundEffectS2CPacket extends JavaS2CPacket {

  private String name;
  private SoundVec3 soundPos;
  private float volume;
  private float pitch;

  public JavaNamedSoundEffectS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    name = readJavaString(buf);
    int fx = buf.readInt();
    int fy = buf.readInt();
    int fz = buf.readInt();
    volume = buf.readFloat();
    pitch = buf.readUnsignedByte() / 63f;
    soundPos = new SoundVec3(fx, fy, fz, getConsoleSoundId(name));
  }

  @Override
  public void handle(JavaSession session) {
    if (soundPos.getSoundId() == -1) {
      return;
    }
    ConsoleSession cs = session.getConsoleSession();

    int cx = soundPos.x();
    int cz = soundPos.z();

    if (cs.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
      cx -= cs.getWorldOffsetX() * 8;
      cz -= cs.getWorldOffsetZ() * 8;
    }

    SoundVec3 adjusted = new SoundVec3(cx, soundPos.y(), cz, soundPos.getSoundId());
    PacketManager.sendToConsole(cs, new ConsoleLevelSoundS2CPacket(adjusted, volume, pitch));
  }
}