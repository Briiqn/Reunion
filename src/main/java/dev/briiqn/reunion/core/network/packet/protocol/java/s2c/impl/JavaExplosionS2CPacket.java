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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleExplodeS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import dev.briiqn.reunion.core.util.math.vector.Vec3f;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x27, supports = {47})
public final class JavaExplosionS2CPacket extends JavaS2CPacket {

  private final List<byte[]> records = new ArrayList<>();
  private float x, y, z, radius;
  private float motionX, motionY, motionZ;

  public JavaExplosionS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    x = buf.readFloat();
    y = buf.readFloat();
    z = buf.readFloat();
    radius = buf.readFloat();

    int count = buf.readInt();
    for (int i = 0; i < count; i++) {
      byte[] record = new byte[3];
      buf.readBytes(record);
      records.add(record);
    }

    motionX = buf.readFloat();
    motionY = buf.readFloat();
    motionZ = buf.readFloat();
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    double cx = cs.toConsoleX(x);
    double cz = cs.toConsoleZ(z);
    PacketManager.sendToConsole(cs,
        new ConsoleExplodeS2CPacket(
            new Vec3d(cx, y, cz),
            radius, records,
            new Vec3f(motionX, motionY, motionZ)
        ));
  }
}