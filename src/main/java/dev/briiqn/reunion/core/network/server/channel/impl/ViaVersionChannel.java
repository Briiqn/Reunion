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

package dev.briiqn.reunion.core.network.server.channel.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaCustomPayloadC2SPacket;
import dev.briiqn.reunion.core.network.server.channel.PluginChannel;
import dev.briiqn.reunion.core.session.ConsoleSession;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ViaVersionChannel extends PluginChannel {

  public ViaVersionChannel() {
    super("vv:server_details", "vv:proxy_details");
  }

  @Override
  public void handleIncoming(ConsoleSession session, String channel, byte[] data) {
    if ("vv:server_details".equals(channel)) {
      handleServerDetails(session, data);
    }
  }

  private void handleServerDetails(ConsoleSession session, byte[] data) {
    try {
      String json = new String(data, StandardCharsets.UTF_8);
      JSONObject obj = JSON.parseObject(json);
      int protocol = obj.getIntValue("version");
      session.setBackendProtocol(protocol);
      JSONObject proxyInfo = new JSONObject();
      proxyInfo.put("specVersion", 1);
      proxyInfo.put("platformName", "Reunion");
      proxyInfo.put("platformVersion", ReunionServer.releaseVersion);
      proxyInfo.put("version", session.getClientVersion());
      proxyInfo.put("versionName", String.valueOf(session.getClientVersion()));
      proxyInfo.put("versionType", "RELEASE");
      byte[] replyBytes = proxyInfo.toJSONString().getBytes(StandardCharsets.UTF_8);
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaCustomPayloadC2SPacket("vv:proxy_details", Unpooled.wrappedBuffer(replyBytes)));
    } catch (Exception e) {
      log.warn("[ViaVersion] Failed to parse vv:server_details: {}", e.getMessage());
    }
  }
}