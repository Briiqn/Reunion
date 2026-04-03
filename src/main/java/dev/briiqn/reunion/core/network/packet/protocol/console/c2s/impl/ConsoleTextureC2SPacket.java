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

package dev.briiqn.reunion.core.network.packet.protocol.console.c2s.impl;

import dev.briiqn.reunion.core.data.Geometry;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.DisconnectReason;
import dev.briiqn.reunion.core.network.packet.data.enums.TextureChangeType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureAndGeometryS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureChangeS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureS2CPacket;
import dev.briiqn.reunion.core.network.server.channel.impl.ReunionInfoChannel;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.math.BoundingBox;
import dev.briiqn.reunion.core.util.math.MathUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 154, supports = {39, 78})
public final class ConsoleTextureC2SPacket extends ConsoleC2SPacket {

  private String textureName;
  private byte[] data;

  public ConsoleTextureC2SPacket() {
  }

  public static double calculateModelSize(byte[] boxData, int boxCount) {
    if (boxCount == 0 || boxData == null) {
      return 0.0;
    }
    BoundingBox bounds = BoundingBox.EMPTY;
    ByteBuf buf = Unpooled.wrappedBuffer(boxData);
    try {
      for (int i = 0; i < boxCount; i++) {
        if (buf.readableBytes() < 34) {
          break;
        }
        buf.readShort();
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float w = buf.readFloat();
        float h = buf.readFloat();
        float d = buf.readFloat();
        buf.skipBytes(8);
        bounds = bounds.union(new BoundingBox(x, y, z, x + w, y + h, z + d));
      }
    } finally {
      buf.release();
    }
    return MathUtil.max(bounds.getXLength(), bounds.getYLength(), bounds.getZLength()) / 16.0;
  }

  public static boolean isValidModelSize(ConsoleSession session, byte[] boxData, int boxCount) {
    if (boxCount <= 0 || boxData == null) {
      return true;
    }
    double size = calculateModelSize(boxData, boxCount);
    var gp = session.getServer().getConfig().getGameplay();
    return size >= (gp.getMinModelSizeBlocks() - 0.01) && size <= (gp.getMaxModelSizeBlocks()
        + 0.01);
  }

  static void handleRequest(ConsoleSession session, String textureName, int dwSkinID) {
    var server = session.getServer();
    if (server.isLceTextureRejected(textureName)) {
      log.warn("[Reunion] Ignored request for rejected texture '{}' from {}",
          textureName, session.getPlayerName());
      return;
    }
    Geometry cachedGeo = server.getCachedGeometry(textureName);
    if (cachedGeo != null) {
      if (!isValidModelSize(session, cachedGeo.boxData(), cachedGeo.boxCount())) {
        session.queueOrSendChat(
            "[Reunion] The skin you have selected is not allowed on this server, other players will not see your currently active skin.");

        server.evictLceTexture(textureName);
        return;
      }
      PacketManager.sendToConsole(session,
          new ConsoleTextureAndGeometryS2CPacket(textureName, cachedGeo.dwSkinID(),
              cachedGeo.textureData(), cachedGeo.animOverride(), cachedGeo.boxData(),
              cachedGeo.boxCount()));
      return;
    }
    byte[] cachedPlain = server.getCachedLceTexture(textureName);
    if (cachedPlain != null) {
      PacketManager.sendToConsole(session, new ConsoleTextureS2CPacket(textureName, cachedPlain));
      return;
    }
    boolean firstWaiter = server.addLceTexturePending(textureName, session);
    if (firstWaiter) {
      ConsoleSession owner = server.getLceTextureOwner(textureName);
      if (owner != null && owner != session) {
        PacketManager.sendToConsole(owner,
            new ConsoleTextureAndGeometryS2CPacket(textureName, dwSkinID));
      }
    }
  }

  public static void handleDelivery(ConsoleSession session, String textureName, byte[] pixels,
      int dwSkinID, byte[] boxData, int boxCount, int animOverride) {
    var server = session.getServer();
    var config = server.getConfig().getGameplay();

    int totalBytes = (pixels != null ? pixels.length : 0) + (boxData != null ? boxData.length : 0);
    if (totalBytes > config.getMaxModelByteSize()) {
      log.warn("[Reunion] Kicking {}: Payload {} > limit {}", session.getPlayerName(), totalBytes,
          config.getMaxModelByteSize());
      PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket(DisconnectReason.KICKED));
      session.getConsoleChannel().close();
      return;
    }

    if (!isValidModelSize(session, boxData, boxCount)) {
      session.queueOrSendChat(
          "[Reunion] The skin you have selected is not allowed on this server, other players will not see your currently active skin.");

      server.evictLceTexture(textureName);
      TextureChangeType type =
          textureName.contains("cape") ? TextureChangeType.CAPE : TextureChangeType.SKIN;
      if (type == TextureChangeType.SKIN) {
        session.setLceSkinName(null);
        session.setLceSkinDwId(0);
      } else {
        session.setLceCapeName(null);
      }
      for (ConsoleSession peer : server.getSessions().values()) {
        Integer cid = peer.getPlayerManager().getConsoleId(session.getUuid());
        if (cid != null) {
          PacketManager.sendToConsole(peer, new ConsoleTextureChangeS2CPacket(cid, type, ""));
        }
      }
      return;
    }

    UUID ownerUuid = session.getUuid();
    TextureChangeType textureType =
        textureName.contains("cape") ? TextureChangeType.CAPE : TextureChangeType.SKIN;
    if (boxCount > 0 && boxData != null) {
      Geometry geo = new Geometry(dwSkinID, pixels, animOverride, boxData, boxCount);
      Set<ConsoleSession> waiters = server.deliverGeometry(textureName, geo);
      for (ConsoleSession waiter : waiters) {
        PacketManager.sendToConsole(waiter,
            new ConsoleTextureAndGeometryS2CPacket(textureName, dwSkinID, pixels, animOverride,
                boxData, boxCount));
      }
      if (ownerUuid != null) {
        for (ConsoleSession cs : server.getSessions().values()) {
          if (waiters.contains(cs) || cs == session) {
            continue;
          }
          Integer cid = cs.getPlayerManager().getConsoleId(ownerUuid);
          if (cid != null) {
            PacketManager.sendToConsole(cs,
                new ConsoleTextureAndGeometryS2CPacket(textureName, dwSkinID, pixels, animOverride,
                    boxData, boxCount));
            PacketManager.sendToConsole(cs,
                new ConsoleTextureChangeS2CPacket(cid, textureType, textureName));
          }
        }
      }
    } else {
      Set<ConsoleSession> waiters = server.deliverLceTexture(textureName, pixels);
      for (ConsoleSession waiter : waiters) {
        PacketManager.sendToConsole(waiter, new ConsoleTextureS2CPacket(textureName, pixels));
      }
      if (ownerUuid != null) {
        for (ConsoleSession cs : server.getSessions().values()) {
          if (waiters.contains(cs) || cs == session) {
            continue;
          }
          Integer cid = cs.getPlayerManager().getConsoleId(ownerUuid);
          if (cid != null) {
            PacketManager.sendToConsole(cs, new ConsoleTextureS2CPacket(textureName, pixels));
            PacketManager.sendToConsole(cs,
                new ConsoleTextureChangeS2CPacket(cid, textureType, textureName));
          }
        }
      }
    }

    if (session.getLceSkinName() != null && session.getLceSkinName().equals(textureName)) {
      ReunionInfoChannel.forward(session);
    } else if (session.getLceCapeName() != null && session.getLceCapeName().equals(textureName)) {
      ReunionInfoChannel.forward(session);
    }
  }

  @Override
  public void read(ByteBuf buf) {
    textureName = StringUtil.readConsoleModifiedUtf8(buf);
    int len = buf.readUnsignedShort();
    if (len > 0) {
      data = new byte[len];
      buf.readBytes(data);
    } else {
      data = null;
    }
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    if (textureName == null || textureName.isEmpty()) {
      return;
    }
    if (data == null) {
      handleRequest(session, textureName, 0);
    } else {
      handleDelivery(session, textureName, data, 0, null, 0, 0);
    }
  }
}