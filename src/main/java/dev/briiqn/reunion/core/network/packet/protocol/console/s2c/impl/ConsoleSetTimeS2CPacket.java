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

package dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 4, supports = {39, 78})
public final class ConsoleSetTimeS2CPacket extends ConsoleS2CPacket {

  private final long gameTime;
  private final long dayTime;

  public ConsoleSetTimeS2CPacket() {
    this(0L, 0L);
  }

  public ConsoleSetTimeS2CPacket(long gameTime, long dayTime) {
    this.gameTime = gameTime;
    this.dayTime = dayTime;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeLong(gameTime);
    buf.writeLong(dayTime);
  }
}