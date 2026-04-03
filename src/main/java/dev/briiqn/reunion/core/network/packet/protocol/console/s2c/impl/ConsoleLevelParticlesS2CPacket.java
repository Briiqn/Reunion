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
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.math.vector.Vec3f;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 63, supports = {39, 78})
public final class ConsoleLevelParticlesS2CPacket extends ConsoleS2CPacket {

  private final String name;
  private final Vec3f origin;
  private final Vec3f spread;
  private final float maxSpeed;
  private final int count;

  public ConsoleLevelParticlesS2CPacket() {
    this("", Vec3f.ZERO, Vec3f.ZERO, 0f, 0);
  }

  public ConsoleLevelParticlesS2CPacket(String name, Vec3f origin, Vec3f spread,
      float maxSpeed, int count) {
    this.name = name;
    this.origin = origin;
    this.spread = spread;
    this.maxSpeed = maxSpeed;
    this.count = count;
  }

  @Deprecated
  public ConsoleLevelParticlesS2CPacket(String name, float x, float y, float z,
      float xDist, float yDist, float zDist, float maxSpeed, int count) {
    this(name, new Vec3f(x, y, z), new Vec3f(xDist, yDist, zDist), maxSpeed, count);
  }

  public String name() {
    return name;
  }

  public Vec3f origin() {
    return origin;
  }

  public Vec3f spread() {
    return spread;
  }

  public float maxSpeed() {
    return maxSpeed;
  }

  public int count() {
    return count;
  }

  @Override
  public void write(ByteBuf buf) {
    StringUtil.writeConsoleUtf(buf, name, 64);
    buf.writeFloat(origin.x());
    buf.writeFloat(origin.y());
    buf.writeFloat(origin.z());
    buf.writeFloat(spread.x());
    buf.writeFloat(spread.y());
    buf.writeFloat(spread.z());
    buf.writeFloat(maxSpeed);
    buf.writeInt(count);
  }
}