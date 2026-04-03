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
import dev.briiqn.reunion.core.util.math.vector.Vec3d;

@PacketInfo(side = PacketSide.JAVA_C2S, id = 0x04, supports = {47})
public final class JavaPlayerPositionC2SPacket extends JavaPlayerFlyingC2SPacket {

  public JavaPlayerPositionC2SPacket() {
    this.hasPos = true;
    this.hasRot = false;
  }

  public JavaPlayerPositionC2SPacket(double x, double y, double z, boolean onGround) {
    super(onGround);
    this.pos = new Vec3d(x, y, z);
    this.hasPos = true;
    this.hasRot = false;
  }
}
