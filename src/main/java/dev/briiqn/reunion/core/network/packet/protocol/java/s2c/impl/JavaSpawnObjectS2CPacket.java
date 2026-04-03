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
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddEntityS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddMobS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleSetEntityDataS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.registry.impl.EntityRegistry;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Set;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x0E, supports = {47})
public final class JavaSpawnObjectS2CPacket extends JavaS2CPacket {

  private static final Set<Integer> PROJECTILE_TYPES = Set.of(
      EntityType.ARROW.getId(), EntityType.SNOWBALL.getId(), EntityType.EGG.getId(),
      EntityType.FIREBALL.getId(), EntityType.SMALL_FIREBALL.getId(),
      EntityType.ENDER_PEARL.getId(), EntityType.WITHER_SKULL.getId(),
      EntityType.EYE_OF_ENDER.getId(), EntityType.POTION.getId(),
      EntityType.EXP_BOTTLE.getId(), EntityType.FISHING_HOOK.getId()
  );

  private int entityId, type, x, y, z, data;
  private byte pitch, yaw;
  private short speedX, speedY, speedZ;

  @Override
  public void read(ByteBuf buf) {
    entityId = VarIntUtil.read(buf);
    type = buf.readByte();
    x = buf.readInt();
    y = buf.readInt();
    z = buf.readInt();
    pitch = buf.readByte();
    yaw = buf.readByte();
    data = buf.readInt();

    if (data > 0) {
      speedX = buf.readShort();
      speedY = buf.readShort();
      speedZ = buf.readShort();
    }
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    var em = cs.getEntityManager();
    int cid = em.map(entityId);

    em.setPosition(entityId, x, y, z);
    em.registerCategory(entityId,
        dev.briiqn.reunion.core.manager.EntityManager.EntityCategory.OBJECT);
    em.registerType(entityId, type);

    int cx = x, cz = z;
    if (cs.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
      cx -= cs.getWorldOffsetX() * 32;
      cz -= cs.getWorldOffsetZ() * 32;
    }

    if (type == EntityType.ARMOR_STAND.getId()) {
      em.setRotation(entityId, yaw, pitch);
      PacketManager.sendToConsole(cs, new ConsoleAddMobS2CPacket(
          cid, EntityType.ZOMBIE.getId(), cx, y, cz, yaw, pitch, yaw,
          speedX, speedY, speedZ, (byte) 0
      ));

      Entity entity = EntityRegistry.getInstance().get(Entity.Type.OBJECT, type);
      String displayName = entity != null ? entity.displayName() : "Armor Stand";

      ByteBuf out = Unpooled.buffer();
      out.writeByte((4 << 5) | 10);
      StringUtil.writeConsoleUtf(out, displayName, 64);
      out.writeByte((0 << 5) | 11);
      out.writeByte(1);
      out.writeByte(127);

      byte[] meta = new byte[out.readableBytes()];
      out.readBytes(meta);
      out.release();

      PacketManager.sendToConsole(cs, new ConsoleSetEntityDataS2CPacket(cid, meta));
      return;
    }

    em.setRotation(entityId, yaw, pitch);
    int cData = data;

    if (type == EntityType.FALLING_BLOCK.getId()) {
      int[] remapped = BlockRegistry.getInstance().remapBlock(data & 0xFFF, data >> 12);
      cData = remapped[0] | (remapped[1] << 12);
    } else if (PROJECTILE_TYPES.contains(type)) {
      cData = data > 0 ? em.map(data) : cData;
    } else if (data == 0 && type != EntityType.ITEM_FRAME.getId()) {
      cData = -1;
    }

    em.registerObjectData(entityId, cData);
    PacketManager.sendToConsole(cs, new ConsoleAddEntityS2CPacket(
        cid, type, cx, y, cz, yaw, pitch, cData, speedX, speedY, speedZ
    ));
  }
}