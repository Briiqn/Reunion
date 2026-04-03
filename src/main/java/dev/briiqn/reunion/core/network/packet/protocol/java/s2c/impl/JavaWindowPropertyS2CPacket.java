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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleContainerSetDataS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x31, supports = {47})
public final class JavaWindowPropertyS2CPacket extends JavaS2CPacket {

  private byte windowId;
  private short property, value;

  public JavaWindowPropertyS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    windowId = buf.readByte();
    property = buf.readShort();
    value = buf.readShort();
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    int windowType = cs.getWindowType(windowId);

    short translatedProperty = property;
    short translatedValue = value;

    // Furnace
    if (windowType == 2) {
      switch (property) {
        case 2: // Java Cook Time -> LCE Arrow Current
          translatedProperty = 0;
          break;
        case 3: // Java Cook Total -> LCE Arrow Max
          translatedProperty = 1;
          break;

        case 1: // Java Burn Total
          cs.setFurnaceBurnTotal(value);
          translatedProperty = 3;        // LCE Flame Max
          break;
        case 0: // Java Burn Remaining
          // LCE expects ELAPSED time for the flame to dec.
          int elapsed = cs.getFurnaceBurnTotal() - value;
          translatedValue = (short) Math.max(0, elapsed);
          translatedProperty = 2; // LCE Flame Current
          break;

        default:
          return;
      }
    }

    PacketManager.sendToConsole(cs,
        new ConsoleContainerSetDataS2CPacket(windowId, translatedProperty, translatedValue));
  }
}