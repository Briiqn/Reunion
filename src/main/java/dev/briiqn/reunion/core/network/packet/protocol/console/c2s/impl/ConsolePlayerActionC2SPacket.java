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
import dev.briiqn.reunion.core.network.packet.data.enums.PlayerAction;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaAnimateC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaPlayerDiggingC2SPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 14, supports = {39, 78})
public final class ConsolePlayerActionC2SPacket extends ConsoleC2SPacket {

  private PlayerAction action;
  private int x, y, z, face;

  public ConsolePlayerActionC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    action = PlayerAction.fromId(buf.readUnsignedByte());
    x = buf.readInt();
    y = buf.readUnsignedByte();
    z = buf.readInt();
    face = buf.readUnsignedByte();
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    if (action == null) {
      return;
    }

    final int javaX = session.toJavaX(x);
    final int javaZ = session.toJavaZ(z);

    if (!PluginEventHooks.fireAction(new ConsoleSessionPlayerAdapter(session), action.getId(),
        javaX, y, javaZ, face)) {
      return;
    }

    if (action == PlayerAction.DROP_ALL_ITEMS || action == PlayerAction.DROP_ITEM
        || action == PlayerAction.RELEASE_USE_ITEM) {
      if (action == PlayerAction.RELEASE_USE_ITEM) {
        session.setUsingItem(false);
      }
      // MUST be 0, 0, 0 and face 0 for drops/releases in 1.8
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaPlayerDiggingC2SPacket(action.getId(), 0, 0, 0, 0));
      return;
    }

    // If the player punches a block while holding an item, we MUST unblock.
    boolean releaseItemOnInteract = session.getServer().getConfig().getGameplay()
        .isReleaseItemOnInteract();
    if (releaseItemOnInteract && session.isUsingItem()) {
      // Send RELEASE_USE_ITEM (5)
      // MUST use X=0, Y=0, Z=0, Face=0
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaPlayerDiggingC2SPacket(PlayerAction.RELEASE_USE_ITEM.getId(), 0, 0, 0, 0));
      session.setUsingItem(false);
    }

    // 1.8 always sends DOWN (0) when aborting a break
    if (action == PlayerAction.ABORT_DESTROY_BLOCK) {
      face = 0;
    }

    final PlayerAction finalAction = action;
    final int finalFace = face;
    final int finalY = y;

    session.queueTickAction(() -> {
      if (finalAction == PlayerAction.START_DESTROY_BLOCK
          || finalAction == PlayerAction.STOP_DESTROY_BLOCK) {
        PacketManager.sendToJava(session.getJavaSession(), new JavaAnimateC2SPacket());
      }
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaPlayerDiggingC2SPacket(finalAction.getId(), javaX, finalY, javaZ, finalFace));
    });
  }
}