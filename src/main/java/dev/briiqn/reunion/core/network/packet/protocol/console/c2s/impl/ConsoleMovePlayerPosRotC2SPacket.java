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

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 13, supports = {39, 78})
public final class ConsoleMovePlayerPosRotC2SPacket extends ConsolePlayerFlyingC2SPacket {

  public ConsoleMovePlayerPosRotC2SPacket() {
    this.hasPos = true;
    this.hasRot = true;
  }

  @Override
  public void read(ByteBuf buf) {
    double x = buf.readDouble();
    double y = buf.readDouble();
    stance = buf.readDouble();          // offset eyes
    double z = buf.readDouble();
    pos = new Vec3d(x, y, z);
    rot = new Vec2f(buf.readFloat(), buf.readFloat());
    onGround = (buf.readByte() & 1) != 0;
  }
}
