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
import dev.briiqn.reunion.core.network.packet.data.enums.PlayerCommand;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaEntityActionC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 19, supports = {39, 78})
public final class ConsolePlayerCommandC2SPacket extends ConsoleC2SPacket {

  private int entityId, action, data;

  @Override
  public void read(ByteBuf buf) {
    entityId = buf.readInt();
    action = buf.readUnsignedByte();
    data = buf.readInt();
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    PlayerCommand cmdAction = PlayerCommand.fromConsoleId(action);

    if (cmdAction != null) {
      switch (cmdAction) {
        case START_SNEAKING -> session.setSneaking(true);
        case STOP_SNEAKING -> session.setSneaking(false);
        case START_SPRINTING -> session.setSprinting(true);
        case STOP_SPRINTING -> session.setSprinting(false);
        default -> {
        }
      }

      int jumpBoost = (cmdAction == PlayerCommand.RIDING_JUMP) ? data : 0;
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaEntityActionC2SPacket(session.getJavaEntityId(), cmdAction.getJavaId(),
              jumpBoost));
    }
  }
}
