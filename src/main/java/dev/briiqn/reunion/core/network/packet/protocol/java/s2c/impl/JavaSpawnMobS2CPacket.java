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

import dev.briiqn.reunion.core.data.Entity;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.EntityType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddMobS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleSetEntityDataS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.EntityRegistry;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.VarIntUtil;
import dev.briiqn.reunion.core.util.game.ItemUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x0F, supports = {47})
public final class JavaSpawnMobS2CPacket extends JavaS2CPacket {

  private int entityId, type, x, y, z;
  private byte yaw, pitch, headYaw;
  private short vx, vy, vz;
  private byte flags;
  private byte[] metadata;

  @Override
  public void read(ByteBuf buf) {
    entityId = VarIntUtil.read(buf);
    type = buf.readUnsignedByte();
    x = buf.readInt();
    y = buf.readInt();
    z = buf.readInt();
    yaw = buf.readByte();
    pitch = buf.readByte();
    headYaw = buf.readByte();
    vx = buf.readShort();
    vy = buf.readShort();
    vz = buf.readShort();

    int start = buf.readerIndex();
    flags = ItemUtil.readMetadataFlags(buf);

    buf.readerIndex(start);
    metadata = new byte[buf.readableBytes()];
    buf.readBytes(metadata);
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    var em = cs.getEntityManager();
    var registry = EntityRegistry.getInstance();

    em.registerCategory(entityId, dev.briiqn.reunion.core.manager.EntityManager.EntityCategory.MOB);
    em.registerType(entityId, type);
    em.setRotation(entityId, yaw, pitch);
    em.setPosition(entityId, x, y, z);

    int cid = em.map(entityId);
    int cType = type;
    boolean remapped = false;

    if (type == 30) {
      cType = 54;
      remapped = true;
    } else if (registry.get(type) == null || registry.isUnsupported(type)) {
      cType = registry.remap(type);
      if (registry.get(cType) == null) {
        cType = 54;
      }
      remapped = true;
    }

    int cx = x, cz = z;
    if (cs.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
      cx -= cs.getWorldOffsetX() * 32;
      cz -= cs.getWorldOffsetZ() * 32;
    }

    PacketManager.sendToConsole(cs, new ConsoleAddMobS2CPacket(
        cid, cType, cx, y, cz, yaw, pitch, headYaw, vx, vy, vz, flags
    ));

    Entity entity = registry.get(type);
    String defaultName = entity != null ? entity.displayName() : "Entity ID " + type;

    ByteBuf in = metadata != null ? Unpooled.wrappedBuffer(metadata) : Unpooled.EMPTY_BUFFER;
    ByteBuf out = Unpooled.buffer();

    try {
      String serverCustomName = null;
      Byte serverCustomNameVisible = null;

      while (in.isReadable()) {
        int header = in.readUnsignedByte();
        if (header == 127) {
          break;
        }

        int mType = header >> 5;
        int idx = header & 0x1F;

        if (idx == 2 && mType == 4) {
          serverCustomName = StringUtil.readJavaString(in);
          continue;
        } else if (idx == 3 && mType == 0) {
          serverCustomNameVisible = in.readByte();
          continue;
        }

        if (mType > 5) {
          if (mType == 6 || mType == 7) {
            in.skipBytes(12);
          }
          continue;
        }

        if (idx == 8 && mType == 5) {
          idx = 2;
        } else if (idx == 9 && mType == 0) {
          idx = 3;
        }

        boolean isBabyParam = (idx == 12 && mType == 0);
        boolean isZombieOrPigman = (type == EntityType.ZOMBIE.getId()
            || type == EntityType.PIG_ZOMBIE.getId()
            || type == EntityType.ARMOR_STAND.getId());

        if (isBabyParam && !isZombieOrPigman) {
          byte val = in.readByte();
          out.writeByte((2 << 5) | (12 & 0x1F));
          out.writeInt(val < 0 ? -24000 : 0);
          continue;
        }

        out.writeByte((mType << 5) | (idx & 0x1F));
        switch (mType) {
          case 0 -> out.writeByte(in.readByte());
          case 1 -> out.writeShort(in.readShort());
          case 2 -> out.writeInt(in.readInt());
          case 3 -> out.writeFloat(in.readFloat());
          case 4 -> StringUtil.writeConsoleUtf(out, StringUtil.readJavaString(in), 64);
          case 5 -> ItemUtil.writeConsoleItemAuto(out, ItemUtil.readJavaItem(in));
        }
      }

      if (remapped && (serverCustomName == null || serverCustomName.isEmpty())) {
        serverCustomName = defaultName;
        if (serverCustomNameVisible == null) {
          serverCustomNameVisible = 1;
        }
      }

      if (serverCustomName != null) {
        out.writeByte((4 << 5) | 10);
        StringUtil.writeConsoleUtf(out, serverCustomName, 64);
      }
      if (serverCustomNameVisible != null) {
        out.writeByte((0 << 5) | 11);
        out.writeByte(serverCustomNameVisible);
      }

      out.writeByte(127);

      byte[] meta = new byte[out.readableBytes()];
      out.readBytes(meta);
      PacketManager.sendToConsole(cs, new ConsoleSetEntityDataS2CPacket(cid, meta));
    } finally {
      in.release();
      out.release();
    }
  }
}