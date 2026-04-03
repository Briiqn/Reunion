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

import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import io.netty.buffer.ByteBuf;

public abstract class JavaPlayerFlyingC2SPacket extends JavaC2SPacket {

  protected Vec3d pos = Vec3d.ZERO;
  protected Vec2f rot = Vec2f.ZERO;
  protected boolean onGround;
  protected boolean hasPos, hasRot;

  protected JavaPlayerFlyingC2SPacket() {
  }

  protected JavaPlayerFlyingC2SPacket(boolean onGround) {
    this.onGround = onGround;
  }

  @Override
  public void write(ByteBuf buf) {
    if (hasPos) {
      buf.writeDouble(pos.x());
      buf.writeDouble(pos.y());
      buf.writeDouble(pos.z());
    }
    if (hasRot) {
      buf.writeFloat(rot.yaw());
      buf.writeFloat(rot.pitch());
    }
    buf.writeBoolean(onGround);
  }
}
