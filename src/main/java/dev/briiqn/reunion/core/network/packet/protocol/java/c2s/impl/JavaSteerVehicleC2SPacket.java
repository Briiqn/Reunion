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

package dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_C2S, id = 0x0C, supports = {47})
public final class JavaSteerVehicleC2SPacket extends JavaC2SPacket {

  private final float sideways, forward;
  private final byte flags;

  public JavaSteerVehicleC2SPacket() {
    this(0, 0, (byte) 0);
  }

  public JavaSteerVehicleC2SPacket(float sideways, float forward, byte flags) {
    this.sideways = sideways;
    this.forward = forward;
    this.flags = flags;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeFloat(sideways);
    buf.writeFloat(forward);
    buf.writeByte(flags);
  }
}
