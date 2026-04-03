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
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaPlayerFlyingC2SPacket;

@PacketInfo(side = PacketSide.JAVA_C2S, id = 0x03, supports = {47})
public final class JavaPlayerC2SPacket extends JavaPlayerFlyingC2SPacket {

  public JavaPlayerC2SPacket() {
    this.hasPos = false;
    this.hasRot = false;
  }

  public JavaPlayerC2SPacket(boolean onGround) {
    super(onGround);
    this.hasPos = false;
    this.hasRot = false;
  }
}
