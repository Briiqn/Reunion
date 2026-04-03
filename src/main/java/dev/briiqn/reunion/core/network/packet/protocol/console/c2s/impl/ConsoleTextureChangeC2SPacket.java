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
import dev.briiqn.reunion.core.network.packet.data.enums.TextureChangeType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureAndGeometryS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureChangeS2CPacket;
import dev.briiqn.reunion.core.network.server.channel.impl.ReunionInfoChannel;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.StringUtil;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 157, supports = {39, 78})
public final class ConsoleTextureChangeC2SPacket extends ConsoleC2SPacket {

  private int entityId;
  private TextureChangeType action;
  private String path;

  public ConsoleTextureChangeC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = buf.readInt();
    action = TextureChangeType.fromId(buf.readByte());
    path = StringUtil.readConsoleModifiedUtf8(buf);
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    if (path == null || path.isEmpty() || action == null) {
      return;
    }
    if (action == TextureChangeType.SKIN && session.getServer().isLceTextureRejected(path)) {
      log.warn("[LCE Skin] Blocked switch to '{}' for {}: texture is on the rejected list.",
          path, session.getPlayerName());
      path = "";
    }
    Geometry cached =
        (action == TextureChangeType.SKIN) ? session.getServer().getCachedGeometry(path) : null;
    if (cached != null && cached.hasGeometry()) {
      if (!ConsoleTextureC2SPacket.isValidModelSize(session, cached.boxData(), cached.boxCount())) {
        log.warn(
            "[LCE Skin] Blocked switch to '{}' for {}: Cached model size invalid. Evicting and forcing Steve.",
            path, session.getPlayerName());
        session.getServer().evictLceTexture(path);
        session.queueOrSendChat(
            "[Reunion] The skin you have selected is not allowed on this server, other players will not see your currently active skin.");

        path = "";
      }
    }

    if (action == TextureChangeType.SKIN) {
      session.setLceSkinName(path);
    } else {
      session.setLceCapeName(path);
    }
    if (!path.isEmpty()) {
      session.getServer().registerTextureOwner(path, session);
    }

    UUID senderUuid = session.getUuid();
    if (senderUuid == null) {
      return;
    }

    for (ConsoleSession other : session.getServer().getSessions().values()) {
      if (other == session) {
        continue;
      }
      Integer cid = other.getPlayerManager().getConsoleId(senderUuid);
      if (cid == null) {
        continue;
      }

      if (cached != null && !path.isEmpty()) {
        PacketManager.sendToConsole(other,
            new ConsoleTextureAndGeometryS2CPacket(path, cached.dwSkinID(), cached.textureData(),
                cached.animOverride(), cached.boxData(), cached.boxCount()));
      }
      PacketManager.sendToConsole(other, new ConsoleTextureChangeS2CPacket(cid, action, path));
    }
    ReunionInfoChannel.forward(session);
  }

}