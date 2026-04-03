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

import static dev.briiqn.reunion.core.util.StringUtil.readJavaString;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.PlayerListItemAction;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsolePlayerInfoS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import dev.briiqn.reunion.core.util.game.SkinUtil;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x38, supports = {47})
public final class JavaPlayerListItemS2CPacket extends JavaS2CPacket {

  private final java.util.Map<UUID, String> pendingTextures = new java.util.HashMap<>();
  private PlayerListItemAction action;
  private int numPlayers;
  private UUID[] uuids;
  private String[] names;

  public JavaPlayerListItemS2CPacket() {
  }


  @Override
  public void read(ByteBuf buf) {
    action = PlayerListItemAction.fromId(VarIntUtil.read(buf));
    numPlayers = VarIntUtil.read(buf);
    uuids = new UUID[numPlayers];
    names = new String[numPlayers];

    for (int i = 0; i < numPlayers; i++) {
      uuids[i] = new UUID(buf.readLong(), buf.readLong());

      if (action == PlayerListItemAction.ADD_PLAYER) {
        names[i] = readJavaString(buf);
        int props = VarIntUtil.read(buf);

        for (int j = 0; j < props; j++) {
          String propName = readJavaString(buf);
          String propValue = readJavaString(buf);
          if (buf.readBoolean()) {
            readJavaString(buf); // signature
          }
          if ("textures".equals(propName)) {
            pendingTextures.put(uuids[i], propValue);
          }
        }
        VarIntUtil.read(buf); // gamemode
        VarIntUtil.read(buf); // ping
        if (buf.readBoolean()) {
          readJavaString(buf); // display name
        }
      } else if (action == PlayerListItemAction.UPDATE_GAMEMODE
          || action == PlayerListItemAction.UPDATE_LATENCY) {
        VarIntUtil.read(buf);
      } else if (action == PlayerListItemAction.UPDATE_DISPLAY_NAME) {
        if (buf.readBoolean()) {
          readJavaString(buf);
        }
      }
    }
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();

    for (int i = 0; i < numPlayers; i++) {
      if (action == PlayerListItemAction.ADD_PLAYER) {
        byte sid = cs.getPlayerManager().addPlayer(uuids[i], names[i]);
        Integer cidx = cs.getPlayerManager().getColorIdx(uuids[i]);
        PacketManager.sendToConsole(cs,
            new ConsolePlayerInfoS2CPacket(sid, cidx.shortValue(), cs.getPlayerPrivileges(), -1));

        if (pendingTextures.containsKey(uuids[i])) {
          parseAndDownloadTextures(session, uuids[i], pendingTextures.get(uuids[i]));
        }

      } else if (action == PlayerListItemAction.REMOVE_PLAYER) {
        Byte sid = cs.getPlayerManager().removePlayer(uuids[i]);
        if (sid != null) {
          PacketManager.sendToConsole(cs,
              new ConsolePlayerInfoS2CPacket(sid, (short) -1, cs.getPlayerPrivileges(), -1));
        }
      }
    }
  }

  private void parseAndDownloadTextures(JavaSession session, UUID uuid, String base64Value) {
    try {
      String decoded = new String(java.util.Base64.getDecoder().decode(base64Value),
          java.nio.charset.StandardCharsets.UTF_8);
      com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(decoded);
      com.alibaba.fastjson2.JSONObject textures = json.getJSONObject("textures");

      if (textures != null) {
        int skinId =
            (int) (uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits()) & 0x7FFFFFE0;

        if (textures.containsKey("SKIN")) {
          com.alibaba.fastjson2.JSONObject skinObj = textures.getJSONObject("SKIN");
          String url = skinObj.getString("url");
          String skinName = String.format("ugcskin%08X.png", skinId);
          session.getConsoleSession().getPlayerManager().setSkin(uuid, skinName);

          // Alex model
          boolean isSlim = false;
          if (skinObj.containsKey("metadata")) {
            com.alibaba.fastjson2.JSONObject metadata = skinObj.getJSONObject("metadata");
            if ("slim".equals(metadata.getString("model"))) {
              isSlim = true;
            }
          }
          downloadAndSendTexture(session, uuid, url, skinName, (byte) 0, isSlim);
        }

        if (textures.containsKey("CAPE")) {
          String url = textures.getJSONObject("CAPE").getString("url");
          String capeName = String.format("ugccape%08X.png", skinId);
          session.getConsoleSession().getPlayerManager().setCape(uuid, capeName);
          downloadAndSendTexture(session, uuid, url, capeName, (byte) 1, false);
        }
      }
    } catch (Exception e) {
      log.error("Failed to parse textures: {}", e.getMessage());
    }
  }

  private void downloadAndSendTexture(JavaSession session, UUID uuid, String url,
      String textureName, byte type, boolean isSlim) {
    Thread.ofVirtual().start(() -> {
      try {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url)).build();
        java.net.http.HttpResponse<byte[]> response = client.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
          return;
        }

        byte[] data = response.body();

        if (type == 0) {
          data = SkinUtil.convertJavaToLCE(data);
        }

        if (data.length > 32767) {
          log.warn("Texture {} is too large ({} bytes), skipping.", textureName, data.length);
          return;
        }

        if (type == 0 && isSlim) {
          data = SkinUtil.adaptSlimToFull(data);
        }

        dev.briiqn.reunion.core.session.ConsoleSession ownerCs = session.getConsoleSession();
        java.util.Set<dev.briiqn.reunion.core.session.ConsoleSession> waiters =
            ownerCs.getServer().deliverLceTexture(textureName, data);
        for (dev.briiqn.reunion.core.session.ConsoleSession waiter : waiters) {
          dev.briiqn.reunion.core.network.packet.manager.PacketManager.sendToConsole(waiter,
              new dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureS2CPacket(
                  textureName, data));
        }

        for (dev.briiqn.reunion.core.session.ConsoleSession cs :
            ownerCs.getServer().getSessions().values()) {
          Integer cid = cs.getPlayerManager().getConsoleId(uuid);
          if (cid != null) {
            dev.briiqn.reunion.core.network.packet.manager.PacketManager.sendToConsole(cs,
                new dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureS2CPacket(
                    textureName, data));
            dev.briiqn.reunion.core.network.packet.manager.PacketManager.sendToConsole(cs,
                new dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureChangeS2CPacket(
                    cid, dev.briiqn.reunion.core.network.packet.data.enums.TextureChangeType.fromId(
                    type), textureName));
          }
        }

        dev.briiqn.reunion.core.network.packet.manager.PacketManager.sendToConsole(ownerCs,
            new dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureS2CPacket(
                textureName, data));

      } catch (Exception e) {
        log.warn("Failed to download texture {}: {}", textureName, e.getMessage());
      }
    });
  }

  @Override
  public void read(ByteBuf buf, int protocol) {
    super.read(buf, protocol);
  }
}