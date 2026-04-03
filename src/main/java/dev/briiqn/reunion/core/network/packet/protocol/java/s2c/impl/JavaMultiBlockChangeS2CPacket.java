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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTileUpdateS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x22, supports = {47})
public final class JavaMultiBlockChangeS2CPacket extends JavaS2CPacket {

  private int chunkX, chunkZ;
  private int[] xs, ys, zs, blockIds, metas;

  public JavaMultiBlockChangeS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    chunkX = buf.readInt();
    chunkZ = buf.readInt();
    int count = VarIntUtil.read(buf);
    xs = new int[count];
    ys = new int[count];
    zs = new int[count];
    blockIds = new int[count];
    metas = new int[count];
    for (int i = 0; i < count; i++) {
      short horiz = buf.readUnsignedByte();
      int y = buf.readUnsignedByte();
      int state = VarIntUtil.read(buf);
      xs[i] = (chunkX * 16) + (horiz >> 4);
      zs[i] = (chunkZ * 16) + (horiz & 0xF);
      ys[i] = y;
      int bid = state >> 4;
      int m = state & 0xF;

      int[] remapped = BlockRegistry.getInstance().remapBlock(bid, m);
      blockIds[i] = remapped[0];
      metas[i] = remapped[1];
    }
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    for (int i = 0; i < xs.length; i++) {
      int cx = cs.toConsoleX(xs[i]);
      int cz = cs.toConsoleZ(zs[i]);
      PacketManager.sendToConsole(cs,
          new ConsoleTileUpdateS2CPacket(cx, ys[i], cz, blockIds[i], metas[i]));
    }
  }
}