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
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import dev.briiqn.reunion.core.util.math.vector.Vec3f;
import io.netty.buffer.ByteBuf;
import java.util.List;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 60, supports = {39, 78})
public final class ConsoleExplodeS2CPacket extends ConsoleS2CPacket {

  private final Vec3d center;
  private final float radius;
  private final List<byte[]> records;
  private final Vec3f knockback;

  public ConsoleExplodeS2CPacket() {
    this(Vec3d.ZERO, 0f, List.of(), Vec3f.ZERO);
  }

  public ConsoleExplodeS2CPacket(Vec3d center, float radius, List<byte[]> records,
      Vec3f knockback) {
    this.center = center;
    this.radius = radius;
    this.records = records;
    this.knockback = knockback;
  }

  @Deprecated
  public ConsoleExplodeS2CPacket(double x, double y, double z, float radius,
      List<byte[]> records, float kx, float ky, float kz) {
    this(new Vec3d(x, y, z), radius, records, new Vec3f(kx, ky, kz));
  }

  public Vec3d center() {
    return center;
  }

  public float radius() {
    return radius;
  }

  public List<byte[]> records() {
    return records;
  }

  public Vec3f knockback() {
    return knockback;
  }

  @Override
  public void write(ByteBuf buf) {
    // m_bKnockbackOnly = false (we want particles / sound / block breaking)
    buf.writeBoolean(false);
    buf.writeDouble(center.x());
    buf.writeDouble(center.y());
    buf.writeDouble(center.z());
    buf.writeFloat(radius);
    buf.writeInt(records.size());
    for (byte[] record : records) {
      buf.writeByte(record[0]);
      buf.writeByte(record[1]);
      buf.writeByte(record[2]);
    }

    buf.writeFloat(knockback.x());
    buf.writeFloat(knockback.y());
    buf.writeFloat(knockback.z());
  }
}