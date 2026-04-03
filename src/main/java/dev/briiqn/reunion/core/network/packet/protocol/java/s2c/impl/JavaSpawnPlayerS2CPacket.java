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

import static dev.briiqn.reunion.core.util.game.ItemUtil.readJavaItem;

import dev.briiqn.reunion.core.data.Geometry;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.TextureChangeType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddPlayerS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsolePlayerInfoS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleSetEquippedItemS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureAndGeometryS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureChangeS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.registry.impl.ItemRegistry;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x0C, supports = {47})
public final class JavaSpawnPlayerS2CPacket extends JavaS2CPacket {

  private int entityId;
  private UUID uuid;
  private int x, y, z;
  private byte yaw, pitch;
  private short currentItem;
  private byte flags;

  public JavaSpawnPlayerS2CPacket() {
  }

  private static short resolveHeldItem(short id) {
    if (id <= 0) {
      return id;
    }
    if (id < 256) {
      BlockRegistry blocks = BlockRegistry.getInstance();
      return blocks.isUnsupported(id) ? (short) blocks.remapBlock(id, 0)[0] : id;
    } else {
      ItemRegistry items = ItemRegistry.getInstance();
      return items.isUnsupported(id) ? (short) items.remap(id, 0) : id;
    }
  }

  public static void sendLceSkin(ConsoleSession cs, int cid,
      String lceSkin, int dwSkinId,
      ConsoleSession owner) {
    sendLceTexture(cs, cid, lceSkin, dwSkinId, owner, TextureChangeType.SKIN);
  }

  public static void sendLceTexture(ConsoleSession cs, int cid,
      String textureName, int dwId,
      ConsoleSession owner, TextureChangeType type) {
    if (type == TextureChangeType.SKIN) {
      Geometry geo = cs.getServer().getCachedGeometry(textureName);
      if (geo != null) {
        PacketManager.sendToConsole(cs, new ConsoleTextureAndGeometryS2CPacket(
            textureName, geo.dwSkinID(),
            geo.textureData(), geo.animOverride(),
            geo.boxData(), geo.boxCount()));
        PacketManager.sendToConsole(cs, new ConsoleTextureChangeS2CPacket(cid, type, textureName));
        return;
      }
    }

    byte[] plain = cs.getServer().getCachedLceTexture(textureName);
    if (plain != null) {
      PacketManager.sendToConsole(cs, new ConsoleTextureS2CPacket(textureName, plain));
      PacketManager.sendToConsole(cs, new ConsoleTextureChangeS2CPacket(cid, type, textureName));
      return;
    }

    boolean firstWaiter = cs.getServer().addLceTexturePending(textureName, cs);
    PacketManager.sendToConsole(cs, new ConsoleTextureChangeS2CPacket(cid, type, textureName));
    if (firstWaiter && owner != null) {
      log.info("[LCE Texture] Prefetching '{}' (type={}) from owner {} for {}",
          textureName, type, owner.getPlayerName(), cs.getPlayerName());
      PacketManager.sendToConsole(owner, new ConsoleTextureAndGeometryS2CPacket(textureName, dwId));
    }
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = VarIntUtil.read(buf);
    uuid = new UUID(buf.readLong(), buf.readLong());
    x = buf.readInt();
    y = buf.readInt();
    z = buf.readInt();
    yaw = buf.readByte();
    pitch = buf.readByte();
    currentItem = buf.readShort();
    flags = readMetadataFlags(buf);
  }

  private byte readMetadataFlags(ByteBuf buf) {
    byte f = 0;
    while (true) {
      int item = buf.readUnsignedByte();
      if (item == 127) {
        break;
      }
      int type = item >> 5;
      int index = item & 0x1F;
      if (index == 0 && type == 0) {
        f = buf.readByte();
        continue;
      }
      switch (type) {
        case 0 -> buf.readByte();
        case 1 -> buf.readShort();
        case 2 -> buf.readInt();
        case 3 -> buf.readFloat();
        case 4 -> buf.skipBytes(VarIntUtil.read(buf));
        case 5 -> readJavaItem(buf);
        case 6 -> {
          buf.readInt();
          buf.readInt();
          buf.readInt();
        }
        case 7 -> {
          buf.readFloat();
          buf.readFloat();
          buf.readFloat();
        }
      }
    }
    return f;
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    var em = cs.getEntityManager();

    int cid = cs.getEntityManager().map(entityId);
    String name = cs.getPlayerManager().getName(uuid);
    short safeItem = resolveHeldItem(currentItem);
    long xuid = uuid.getMostSignificantBits();
    long onlineXuid = uuid.getLeastSignificantBits();

    em.setRotation(entityId, yaw, pitch);
    em.setPosition(entityId, x, y, z);
    em.registerCategory(entityId,
        dev.briiqn.reunion.core.manager.EntityManager.EntityCategory.PLAYER);
    em.setExtraData(entityId, new Object[]{uuid, safeItem, flags});
    int cx = x, cz = z;
    if (cs.getServer().getConfig().getGameplay().isInfiniteWorldHack()) {
      cx -= cs.getWorldOffsetX() * 32;
      cz -= cs.getWorldOffsetZ() * 32;
    }

    String skinName = cs.getPlayerManager().getSkin(uuid);
    String capeName = cs.getPlayerManager().getCape(uuid);

    ConsoleSession peer = null;
    for (ConsoleSession s : cs.getServer().getSessions().values()) {
      UUID su = s.getUuid();
      if (su != null && su.equals(uuid)) {
        peer = s;
        break;
      }
    }

    int lceSkinId = (skinName == null && peer != null) ? peer.getLceSkinDwId() : 0;

    PacketManager.sendToConsole(cs, new ConsoleAddPlayerS2CPacket(
        cid, name, cx, y, cz, yaw, pitch, yaw, safeItem, flags, xuid, onlineXuid,
        lceSkinId, 0));

    Byte rsmid = cs.getPlayerManager().getSmallId(uuid);
    Integer cidx = cs.getPlayerManager().getColorIdx(uuid);
    if (rsmid != null && cidx != null) {
      PacketManager.sendToConsole(cs, new ConsolePlayerInfoS2CPacket(
          rsmid, cidx.shortValue(), session.getConsoleSession().getPlayerPrivileges(), cid));
    }

    if (currentItem > 0) {
      PacketManager.sendToConsole(cs, new ConsoleSetEquippedItemS2CPacket(
          cid, (short) 0, new ItemInstance(safeItem, (byte) 1, (short) 0)));
    }

    cs.getPlayerManager().setConsoleId(uuid, cid);

    if (skinName != null) {
      sendLceTexture(cs, cid, skinName, 0, peer, TextureChangeType.SKIN);
    }
    if (capeName != null) {
      sendLceTexture(cs, cid, capeName, 0, peer, TextureChangeType.CAPE);
    }

    if (skinName == null || capeName == null) {
      if (peer != null) {
        if (skinName == null && peer.getLceSkinName() != null) {
          sendLceSkin(cs, cid, peer.getLceSkinName(), peer.getLceSkinDwId(), peer);
        }
        if (capeName == null && peer.getLceCapeName() != null) {
          sendLceTexture(cs, cid, peer.getLceCapeName(), 0, peer, TextureChangeType.CAPE);
        }
      }
    }
  }
}