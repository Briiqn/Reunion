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

import static dev.briiqn.reunion.core.util.game.BufferUtil.posX;
import static dev.briiqn.reunion.core.util.game.BufferUtil.posY;
import static dev.briiqn.reunion.core.util.game.BufferUtil.posZ;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleLevelEventS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x28, supports = {47})
public final class JavaEffectS2CPacket extends JavaS2CPacket {

  private static final int EFFECT_BLOCK_BREAK = 2001;
  private static final int EFFECT_BLOCK_DUST = 2006;

  private int effectId, data;
  private long pos;
  private boolean global;

  public JavaEffectS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    effectId = buf.readInt();
    pos = buf.readLong();
    data = buf.readInt();
    global = buf.readBoolean();
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    if (effectId == EFFECT_BLOCK_BREAK || effectId == EFFECT_BLOCK_DUST) {
      int[] remapped = BlockRegistry.getInstance().remapBlock(data & 0xFFF, data >> 12);
      data = remapped[0] | (remapped[1] << 12);
    }

    int cx = cs.toConsoleX(posX(pos));
    int cz = cs.toConsoleZ(posZ(pos));

    PacketManager.sendToConsole(cs,
        new ConsoleLevelEventS2CPacket(effectId, cx, posY(pos), cz, data, global));
  }
}