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
import dev.briiqn.reunion.core.manager.EntityManager;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.EntityType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleSetEntityDataS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.EntityRegistry;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.VarIntUtil;
import dev.briiqn.reunion.core.util.game.ItemUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x1C, supports = {47})
public final class JavaEntityMetadataS2CPacket extends JavaS2CPacket {

  private int entityId;
  private ByteBuf rawData;

  public JavaEntityMetadataS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = VarIntUtil.read(buf);
    int readable = buf.readableBytes();
    rawData = Unpooled.buffer(readable);
    rawData.writeBytes(buf);
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    var em = cs.getEntityManager();

    Integer cid = em.getConsoleId(entityId);
    if (cid == null) {
      if (rawData != null) {
        rawData.release();
      }
      return;
    }

    ByteBuf in = rawData;
    ByteBuf out = Unpooled.buffer();

    Integer typeId = em.getType(entityId);
    int entityType = typeId != null ? typeId : -1;

    EntityRegistry registry = EntityRegistry.getInstance();
    EntityManager.EntityCategory category = em.getCategory(entityId);

    boolean isRemapped = false;
    String defaultName = null;

    if (category == EntityManager.EntityCategory.MOB) {
      if (entityType == 30 || registry.get(entityType) == null || registry.isUnsupported(
          entityType)) {
        isRemapped = true;
        Entity entity = registry.get(entityType);
        defaultName = entity != null ? entity.displayName() : "Entity ID " + entityType;
      }
    } else if (category == EntityManager.EntityCategory.OBJECT
        && registry.isUnsupported(Entity.Type.OBJECT, entityType)) {
      isRemapped = true;
      Entity entity = registry.get(Entity.Type.OBJECT, entityType);
      defaultName = entity != null ? entity.displayName() : "Entity ID " + entityType;
    }

    try {
      String serverCustomName = null;
      Byte serverCustomNameVisible = null;

      while (in.isReadable()) {
        int item = in.readUnsignedByte();
        if (item == 127) {
          break;
        }

        int mType = item >> 5;
        int idx = item & 0x1F;

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
        boolean isZombieOrPigman = (entityType == EntityType.ZOMBIE.getId()
            || entityType == EntityType.PIG_ZOMBIE.getId()
            || entityType == EntityType.ARMOR_STAND.getId());

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

      if (serverCustomName != null || serverCustomNameVisible != null) {
        if (isRemapped && serverCustomName != null && serverCustomName.isEmpty()) {
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
      }

      out.writeByte(127);

      byte[] metaBytes = new byte[out.readableBytes()];
      out.readBytes(metaBytes);
      PacketManager.sendToConsole(cs, new ConsoleSetEntityDataS2CPacket(cid, metaBytes));
    } finally {
      in.release();
      out.release();
    }
  }
}