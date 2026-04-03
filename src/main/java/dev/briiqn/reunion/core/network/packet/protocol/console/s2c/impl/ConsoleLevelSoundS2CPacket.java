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

package dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.util.math.vector.SoundVec3;
import io.netty.buffer.ByteBuf;


@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 62, supports = {39, 78})
public final class ConsoleLevelSoundS2CPacket extends ConsoleS2CPacket {

  private final SoundVec3 sound;
  private final float volume;
  private final float pitch;

  public ConsoleLevelSoundS2CPacket() {
    this(new SoundVec3(0, 0, 0, 0), 1f, 1f);
  }

  public ConsoleLevelSoundS2CPacket(SoundVec3 sound, float volume, float pitch) {
    this.sound = sound;
    this.volume = volume;
    this.pitch = pitch;
  }

  @Deprecated
  public ConsoleLevelSoundS2CPacket(int soundId, int x, int y, int z, float volume, float pitch) {
    this(new SoundVec3(x, y, z, soundId), volume, pitch);
  }

  public SoundVec3 sound() {
    return sound;
  }

  public float volume() {
    return volume;
  }

  public float pitch() {
    return pitch;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeInt(sound.getSoundId());
    buf.writeInt(sound.x());
    buf.writeInt(sound.y());
    buf.writeInt(sound.z());
    buf.writeFloat(volume);
    buf.writeFloat(pitch);
  }
}