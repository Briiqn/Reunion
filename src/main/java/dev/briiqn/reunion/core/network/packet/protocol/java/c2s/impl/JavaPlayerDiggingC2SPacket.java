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

import static dev.briiqn.reunion.core.util.game.BufferUtil.writePosition;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_C2S, id = 0x07, supports = {47})
public final class JavaPlayerDiggingC2SPacket extends JavaC2SPacket {

  private final int action, x, y, z, face;

  public JavaPlayerDiggingC2SPacket() {
    this(0, 0, 0, 0, 0);
  }

  public JavaPlayerDiggingC2SPacket(int action, int x, int y, int z, int face) {
    this.action = action;
    this.x = x;
    this.y = y;
    this.z = z;
    this.face = face;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeByte(action);
    writePosition(buf, x, y, z);
    buf.writeByte(face);
  }
}
