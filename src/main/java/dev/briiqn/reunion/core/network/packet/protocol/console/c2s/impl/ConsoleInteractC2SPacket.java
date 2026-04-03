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
import dev.briiqn.reunion.core.network.packet.data.enums.InteractAction;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaUseEntityC2SPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 7, supports = {39, 78})
public final class ConsoleInteractC2SPacket extends ConsoleC2SPacket {

  private int sourceId, targetConsoleId;
  private InteractAction action;

  public ConsoleInteractC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    sourceId = buf.readInt();
    targetConsoleId = buf.readInt();
    action = InteractAction.fromId(buf.readUnsignedByte());
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    if (action == null) {
      return;
    }

    Integer javaId = session.getEntityManager().reverse(targetConsoleId);
    if (javaId != null) {
      boolean releaseItemOnInteract = session.getServer().getConfig().getGameplay()
          .isReleaseItemOnInteract();

      // in 1.8 we cannot interact if we havent released using the current item (e.g food/sword/bow/etc)
      if (releaseItemOnInteract && session.isUsingItem()) {
        return;
      }

      if (action == InteractAction.INTERACT) {
        PacketManager.sendToJava(session.getJavaSession(),
            new JavaUseEntityC2SPacket(javaId, InteractAction.INTERACT_AT.getId(), 0.0f, 0.0f,
                0.0f));
      }

      PacketManager.sendToJava(session.getJavaSession(),
          new JavaUseEntityC2SPacket(javaId, action.getId()));
    }
  }
}