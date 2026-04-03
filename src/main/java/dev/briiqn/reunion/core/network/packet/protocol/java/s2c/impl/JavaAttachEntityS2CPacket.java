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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleSetRidingS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x1B, supports = {47})
public final class JavaAttachEntityS2CPacket extends JavaS2CPacket {

  private int entityId, vehicleId;
  private boolean leash;

  public JavaAttachEntityS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = buf.readInt();
    vehicleId = buf.readInt();
    leash = buf.readBoolean();
  }

  @Override
  public void handle(JavaSession session) {
    if (leash) {
      return;
    }
    var cs = session.getConsoleSession();

    Integer consoleEntityId = cs.getEntityManager().getConsoleId(entityId);
    if (consoleEntityId == null) {
      return;
    }

    if (consoleEntityId == cs.getSafePlayerId()) {
      cs.setRidingEntityId(vehicleId);
    }

    int consoleVehicle = -1;
    if (vehicleId != -1) {
      Integer mappedV = cs.getEntityManager().getConsoleId(vehicleId);
      if (mappedV == null) {
        return;
      }
      consoleVehicle = mappedV;
    }

    PacketManager.sendToConsole(cs, new ConsoleSetRidingS2CPacket(consoleEntityId, consoleVehicle));
  }
}