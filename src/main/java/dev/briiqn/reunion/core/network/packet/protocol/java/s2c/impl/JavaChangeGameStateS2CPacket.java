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
import dev.briiqn.reunion.core.network.packet.data.enums.ConsoleGameEvent;
import dev.briiqn.reunion.core.network.packet.data.enums.GameMode;
import dev.briiqn.reunion.core.network.packet.data.enums.GameState;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleGameEventS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x2B, supports = {47})
public final class JavaChangeGameStateS2CPacket extends JavaS2CPacket {

  private int reason;
  private float value;

  @Override
  public void read(ByteBuf buf) {
    this.reason = buf.readUnsignedByte();
    this.value = buf.readFloat();
  }

  @Override
  public void handle(JavaSession session) {
    GameState state = GameState.fromId(reason);
    if (state == null) {
      return;
    }

    var cs = session.getConsoleSession();

    switch (state) {
      case BEGIN_RAINING -> {
        PacketManager.sendToConsole(cs,
            new ConsoleGameEventS2CPacket(ConsoleGameEvent.START_RAINING, 0));
      }
      case END_RAINING -> {
        PacketManager.sendToConsole(cs,
            new ConsoleGameEventS2CPacket(ConsoleGameEvent.STOP_RAINING, 0));
      }
      case CHANGE_GAME_MODE -> {
        GameMode mode = GameMode.fromId((int) value);
        if (mode == GameMode.SPECTATOR) {
          mode = GameMode.CREATIVE;
        }

        cs.setGamemode((short) mode.getId());

        PacketManager.sendToConsole(cs,
            new ConsoleGameEventS2CPacket(ConsoleGameEvent.CHANGE_GAME_MODE, mode.getId()));
      }
      case ENTER_CREDITS -> {
        PacketManager.sendToConsole(cs,
            new ConsoleGameEventS2CPacket(ConsoleGameEvent.WIN_GAME, 0));
      }
      case DEMO_MESSAGE -> {
        PacketManager.sendToConsole(cs,
            new ConsoleGameEventS2CPacket(ConsoleGameEvent.DEMO_EVENT, 0));
      }
      case ARROW_HIT -> {
        PacketManager.sendToConsole(cs,
            new ConsoleGameEventS2CPacket(ConsoleGameEvent.SUCCESSFUL_BOW_HIT, 0));
      }
    }
  }
}