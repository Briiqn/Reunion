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
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.registry.impl.ItemRegistry;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x2A, supports = {47})
public final class JavaLevelParticlesS2CPacket extends JavaS2CPacket {

  private static final int PARTICLE_ICONCRACK = 36;
  private static final int PARTICLE_BLOCKCRACK = 37;
  private static final int PARTICLE_BLOCKDUST = 38;

  private static final String[] PARTICLE_NAMES = {
      "explode"
  };

  private int particleId;
  private boolean longDistance;
  private float x, y, z;
  private float offsetX, offsetY, offsetZ;
  private float particleData;
  private int particleCount;
  private int[] data;

  public JavaLevelParticlesS2CPacket() {
  }

  private static String resolveParticleName(int id, int[] data) {
    if (id == PARTICLE_ICONCRACK && data.length >= 2) {
      return resolveIconcrack(data[0], data[1]);
    }

    if ((id == PARTICLE_BLOCKCRACK || id == PARTICLE_BLOCKDUST) && data.length >= 1) {
      int blockId = data[0] & 0xFFF;
      int meta = (data[0] >> 12) & 0xF;
      int[] remapped = BlockRegistry.getInstance().remapBlock(blockId, meta);
      return "tilecrack_" + remapped[0] + "_" + remapped[1];
    }

    if (id < 0 || id >= PARTICLE_NAMES.length) {
      return null;
    }
    return PARTICLE_NAMES[id];
  }

  private static String resolveIconcrack(int itemId, int meta) {
    if (itemId > 0 && itemId < 256) {
      int[] remapped = BlockRegistry.getInstance().remapBlock(itemId, meta);
      itemId = remapped[0];
      meta = remapped[1];
    } else if (itemId >= 256) {
      ItemRegistry items = ItemRegistry.getInstance();
      if (items.isUnsupported(itemId)) {
        itemId = items.remap(itemId, meta);
        meta = 0;
      }
    }
    return "iconcrack_" + itemId + "_" + meta;
  }

  @Override
  public void read(ByteBuf buf) {
    particleId = buf.readInt();
    longDistance = buf.readBoolean();
    x = buf.readFloat();
    y = buf.readFloat();
    z = buf.readFloat();
    offsetX = buf.readFloat();
    offsetY = buf.readFloat();
    offsetZ = buf.readFloat();
    particleData = buf.readFloat();
    particleCount = buf.readInt();

    int dataLength = 0;
    if (particleId == PARTICLE_ICONCRACK) {
      dataLength = 2;
    } else if (particleId == PARTICLE_BLOCKCRACK || particleId == PARTICLE_BLOCKDUST) {
      dataLength = 1;
    }

    data = new int[dataLength];
    for (int i = 0; i < dataLength; i++) {
      data[i] = VarIntUtil.read(buf);
    }
  }

  @Override
  public void handle(JavaSession session) {
    String name = resolveParticleName(particleId, data);
    if (name == null) {
      return;
    }
    var cs = session.getConsoleSession();
    float cx = (float) cs.toConsoleX(x);
    float cz = (float) cs.toConsoleZ(z);
    // PacketManager.sendToConsole(cs,
    //     new ConsoleLevelParticlesS2CPacket(name, new Vec3f(cx, y, cz), new Vec3f(offsetX, offsetY, offsetZ), particleData, particleCount));
  }
}