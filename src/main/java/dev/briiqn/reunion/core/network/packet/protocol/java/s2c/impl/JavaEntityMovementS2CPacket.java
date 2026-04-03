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

import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleMoveEntityPosRotS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleMoveEntityPosS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleMoveEntityRotS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleRotateHeadS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import lombok.Getter;

public abstract class JavaEntityMovementS2CPacket extends JavaS2CPacket {

  @Getter
  protected int entityId;
  @Getter
  protected byte dx, dy, dz;
  @Getter
  protected byte yaw, pitch;
  @Getter
  protected boolean onGround;
  protected boolean hasPos, hasRot;

  @Override
  public void handle(JavaSession session) {
    var em = session.getConsoleSession().getEntityManager();
    Integer cid = em.getConsoleId(entityId);
    if (cid == null) {
      return;
    }

    byte deltaYaw = 0, deltaPitch = 0;

    if (hasRot) {
      byte oldYaw = em.getYaw(entityId);
      byte oldPitch = em.getPitch(entityId);
      deltaYaw = (byte) (yaw - oldYaw);
      deltaPitch = (byte) (pitch - oldPitch);
      em.setRotation(entityId, yaw, pitch);
    }

    if (hasPos) {
      em.movePosition(entityId, dx, dy, dz);
    }

    if (hasPos && hasRot) {
      PacketManager.sendToConsole(session.getConsoleSession(),
          new ConsoleMoveEntityPosRotS2CPacket(cid, dx, dy, dz, deltaYaw, deltaPitch));
    } else if (hasPos) {
      PacketManager.sendToConsole(session.getConsoleSession(),
          new ConsoleMoveEntityPosS2CPacket(cid, dx, dy, dz));
    } else if (hasRot) {
      PacketManager.sendToConsole(session.getConsoleSession(),
          new ConsoleMoveEntityRotS2CPacket(cid, deltaYaw, deltaPitch));
    }

    if (hasRot) {
      Integer type = em.getType(entityId);
      if (type != null && type == 78) { // armor stand
        PacketManager.sendToConsole(session.getConsoleSession(),
            new ConsoleRotateHeadS2CPacket(cid, yaw));
      }
    }
  }
}