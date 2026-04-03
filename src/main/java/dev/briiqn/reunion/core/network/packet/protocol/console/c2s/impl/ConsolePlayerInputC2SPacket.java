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
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaSteerVehicleC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 27, supports = {39, 78})
public final class ConsolePlayerInputC2SPacket extends ConsoleC2SPacket {

  private float xxa;
  private float yya;
  private boolean isJumping;
  private boolean isSneaking;

  public ConsolePlayerInputC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    xxa = buf.readFloat();
    yya = buf.readFloat();
    isJumping = buf.readBoolean();
    isSneaking = buf.readBoolean();
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    byte flags = 0;
    if (isJumping) {
      flags |= 0x1;
    }
    if (isSneaking) {
      flags |= 0x2;
    }

    if (session.getRidingEntityId() != -1) {
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaSteerVehicleC2SPacket(xxa, yya, flags));
    }
  }
}