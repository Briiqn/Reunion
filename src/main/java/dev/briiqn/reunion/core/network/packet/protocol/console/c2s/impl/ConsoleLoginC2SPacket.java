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

import dev.briiqn.reunion.api.event.player.LoginEvent;
import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.DisconnectReason;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleLoginS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureAndGeometryS2CPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.game.SkinUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.CONSOLE_C2S, id = 1, supports = {39, 78})
public final class ConsoleLoginC2SPacket extends ConsoleC2SPacket {

  private int clientVersion;
  private String username;
  private int skinId;
  private int capeId;
  private long offlineXuid;
  private long onlineXuid;

  public ConsoleLoginC2SPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    clientVersion = buf.readInt();
    username = StringUtil.readConsoleUtf(buf);
    StringUtil.readConsoleUtf(buf);      // levelType
    buf.readLong();                      // seed
    buf.readInt();                       // gameType
    buf.readByte();                      // dimension
    buf.readByte();                      // mapHeight
    buf.readByte();                      // maxPlayers
    offlineXuid = buf.readLong();        // offlineXuid
    onlineXuid = buf.readLong();         // onlineXuid
    buf.readBoolean();                   // friendsOnlyUGC
    buf.readInt();                       // ugcPlayersVersion
    buf.readByte();                      // difficulty
    buf.readInt();                       // multiplayerInstanceId
    buf.readByte();                      // playerIndex
    skinId = buf.readInt();              // m_playerSkinId
    capeId = buf.readInt();              // m_playerCapeId
    buf.readBoolean();                   // isGuest
    buf.readBoolean();                   // newSeaLevel
    buf.readInt();                       // uiGamePrivileges
    buf.readShort();                     // xzSize
    buf.readByte();                      // hellScale
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {
    if (session.getServer().getBanManager().isBanned(username)) {
      log.info("{} tried to join but is banned for '{}'", username,
          session.getServer().getBanManager().getBanReason(username));
      PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket(DisconnectReason.BANNED));
      session.getConsoleChannel().close();
      return;
    }

    log.info("[Console] Login: version={} username={}", clientVersion, username);
    log.debug("[LCE Skin] {} raw skinId=0x{} capeId=0x{}",
        username,
        Integer.toUnsignedString(skinId, 16).toUpperCase(),
        Integer.toUnsignedString(capeId, 16).toUpperCase());

    session.setClientVersion(clientVersion);
    session.setPlayerName(username);
    session.setXuid(onlineXuid != 0 ? onlineXuid : offlineXuid);

    if (clientVersion > ReunionServer.maxSupportedClientProtocol) {
      log.warn("[Console] Client version {} might not be fully supported (expected 78)",
          clientVersion);
    }

    String skinName = SkinUtil.getSkinPathFromId(skinId);
    String capeName = SkinUtil.getCapePathFromId(capeId);

    if (skinName != null) {
      session.setLceSkinName(skinName);
      session.setLceSkinDwId(skinId);
      session.getServer().registerTextureOwner(skinName, session);
      log.debug("[LCE Skin] {} skin='{}'", username,
          skinName);

      PacketManager.sendToConsole(session,
          new ConsoleTextureAndGeometryS2CPacket(skinName, skinId));
    }

    if (capeName != null) {
      session.setLceCapeName(capeName);
      session.getServer().registerTextureOwner(capeName, session);
      log.debug("[LCE Skin] {} cape='{}'", username,
          capeName);
      PacketManager.sendToConsole(session,
          new ConsoleTextureAndGeometryS2CPacket(capeName, capeId));
    } else {
      log.debug("[LCE Skin] {} capeId=0x{}", username,
          Integer.toUnsignedString(capeId, 16).toUpperCase());
    }

    session.getServer().addSession(username, session);

    var apiPlayer = new ConsoleSessionPlayerAdapter(session);

    if (PluginEventHooks.isHeld(session)) {
      session.initChunkManager();

      int tempSafeId = session.getEntityManager().allocConsoleId();
      session.setSafePlayerId(tempSafeId);

      PacketManager.sendToConsole(session, new ConsoleLoginS2CPacket(
          tempSafeId,
          username.substring(0, Math.min(username.length(), 16)),
          "default", 0L, 0, 0, 2, session.getSmallId(), session.getPlayerPrivileges()
      ));

      session.setLoggedIn(true);
      session.getConsoleChannel().config().setAutoRead(true);
      PluginEventHooks.fireHeld(apiPlayer);
      return;
    }

    LoginEvent event = PluginEventHooks.fireLogin(apiPlayer);
    if (!event.isAllowed()) {
      String reason = event.result().reason().orElse("Denied.");
      log.debug("Login denied for {} by plugin: {}", username, reason);
      PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
      session.getConsoleChannel().close();
      session.getServer().removeSession(username);
    }
  }
}