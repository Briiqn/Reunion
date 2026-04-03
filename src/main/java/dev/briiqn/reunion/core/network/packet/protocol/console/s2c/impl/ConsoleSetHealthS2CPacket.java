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

@PacketInfo(side = PacketSide.CONSOLE_S2C, id = 8, supports = {39, 78})
public final class ConsoleSetHealthS2CPacket extends ConsoleS2CPacket {

  private final float health;
  private final short food;
  private final float saturation;

  public ConsoleSetHealthS2CPacket() {
    this(20f, (short) 20, 5f);
  }

  public ConsoleSetHealthS2CPacket(float health, short food, float saturation) {
    this.health = health;
    this.food = food;
    this.saturation = saturation;
  }

  @Override
  public void write(ByteBuf buf) {
    buf.writeFloat(health);
    buf.writeShort(food);
    buf.writeFloat(saturation);
    buf.writeByte(0);
  }

  public void write39(ByteBuf buf) {
    buf.writeShort((int) health);
    buf.writeShort(food);
    buf.writeFloat(saturation);
    buf.writeByte(0);
  }
}