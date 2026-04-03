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

package dev.briiqn.reunion.core.network.pipeline.java.handler;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.data.ForwardingMode;
import dev.briiqn.reunion.core.network.packet.data.RawPacket;
import dev.briiqn.reunion.core.network.pipeline.java.decode.JavaCipherDecoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaCipherEncoder;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.StringUtil;
import dev.briiqn.reunion.core.util.VarIntUtil;
import dev.briiqn.reunion.core.util.auth.AuthUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.log4j.Log4j2;
import net.raphimc.minecraftauth.java.JavaAuthManager;

@Log4j2
public final class JavaLoginHandler {

  private static final String VELOCITY_CHANNEL = "velocity:player_info";

  private final JavaSession session;
  private final ConsoleSession cs;
  private final ReunionServer server;

  public JavaLoginHandler(JavaSession session) {
    this.session = session;
    this.cs = session.getConsoleSession();
    this.server = session.getServer();
  }

  private static void writeString(ByteBuf buf, String s) {
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    VarIntUtil.write(buf, b.length);
    buf.writeBytes(b);
  }

  void handle(ChannelHandlerContext ctx, RawPacket raw) {
    switch (raw.id()) {
      case 0x01 -> handleEncryptionRequest(ctx, raw.payload());
      case 0x02 -> handleLoginSuccess(raw.payload());
      case 0x00 -> handleLoginDisconnect(ctx, raw.payload());
      case 0x04 -> handleLoginPluginRequest(ctx, raw.payload());
    }
  }

  public void sendHandshake(String host, int port) {
    Thread.ofVirtual().name("java-handshake").start(() -> {
      String loginName = resolveLoginName();
      ForwardingMode mode = server.getConfig().getForwarding().getMode();

      String effectiveHost = host;
      if (mode == ForwardingMode.BUNGEECORD || mode == ForwardingMode.BUNGEEGUARD) {
        effectiveHost = buildBungeeHostname(host, mode);
      }

      ByteBuf hs = session.getJavaChannel().alloc().buffer();
      VarIntUtil.write(hs, JavaSession.JAVA_PROTOCOL);
      StringUtil.writeJavaString(hs, effectiveHost);
      hs.writeShort(port);
      VarIntUtil.write(hs, 2);
      session.getJavaChannel().write(new RawPacket(0x00, hs));

      ByteBuf ls = session.getJavaChannel().alloc().buffer();
      StringUtil.writeJavaString(ls, loginName);
      session.getJavaChannel().writeAndFlush(new RawPacket(0x00, ls));
    });
  }

  private void handleLoginSuccess(ByteBuf buf) {
    try {
      String uuidStr = StringUtil.readJavaString(buf.duplicate());
      String javaName = StringUtil.readJavaString(buf.duplicate());
      UUID uuid = UUID.fromString(uuidStr);
      cs.setUuid(uuid);
      log.info("[JavaSession] Login from {} uuid={}",
          cs.getPlayerName(), uuid);
    } catch (Exception e) {
      log.warn("[JavaSession] Failed to parse Login from UUID for {}: {}",
          cs.getPlayerName(), e.getMessage());
    }
  }

  private void handleLoginDisconnect(ChannelHandlerContext ctx, ByteBuf buf) {
    try {
      String reasonJson = StringUtil.readJavaString(buf);
      String parsedReason = reasonJson;
      try {
        com.alibaba.fastjson2.JSONObject obj =
            com.alibaba.fastjson2.JSON.parseObject(reasonJson);
        if (obj.containsKey("text")) {
          parsedReason = obj.getString("text");
        } else if (obj.containsKey("translate")) {
          parsedReason = obj.getString("translate");
        }
      } catch (Exception ignored) {
      }
      session.setDisconnectReason(parsedReason);
      log.warn("[Disconnect] Java Server rejected login for {}. Reason: {}",
          cs.getPlayerName(), parsedReason);
    } catch (Exception e) {
      log.warn("[Disconnect] Java Server rejected login for {} (0x00, unreadable reason).",
          cs.getPlayerName());
    }
    ctx.close();
  }

  private void handleLoginPluginRequest(ChannelHandlerContext ctx, ByteBuf buf) {
    int messageId = VarIntUtil.read(buf);
    String channel = StringUtil.readJavaString(buf);

    if (!VELOCITY_CHANNEL.equals(channel)) {
      sendPluginFailure(ctx, messageId);
      return;
    }

    if (!server.getConfig().getForwarding().isVelocity()) {
      log.warn("[Velocity] Backend requested modern forwarding but mode is '{}'. "
              + "Set forwarding.mode=VELOCITY to enable it.",
          server.getConfig().getForwarding().getMode());
      sendPluginFailure(ctx, messageId);
      return;
    }

    String secret = server.getConfig().getForwarding().getVelocitySecret();
    if (secret == null || secret.isEmpty()) {
      log.warn("[Velocity] Backend requested modern forwarding but velocity-secret is not set.");
      sendPluginFailure(ctx, messageId);
      return;
    }

    int requestedVersion = buf.isReadable() ? buf.readByte() : 1;
    int responseVersion = Math.min(requestedVersion, 1);

    try {
      String playerIp = ((InetSocketAddress) cs.getConsoleChannel().remoteAddress())
          .getAddress().getHostAddress();
      UUID uuid = resolveUuid();
      String username = resolveLoginName();

      ByteBuf data = Unpooled.buffer();
      VarIntUtil.write(data, responseVersion);
      writeString(data, playerIp);
      data.writeLong(uuid.getMostSignificantBits());
      data.writeLong(uuid.getLeastSignificantBits());
      writeString(data, username);
      VarIntUtil.write(data, 0);

      byte[] dataBytes = new byte[data.readableBytes()];
      data.readBytes(dataBytes);
      data.release();

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] sig = mac.doFinal(dataBytes);

      ByteBuf resp = ctx.alloc().buffer();
      VarIntUtil.write(resp, messageId);
      resp.writeBoolean(true);
      resp.writeBytes(sig);
      resp.writeBytes(dataBytes);

      ctx.writeAndFlush(new RawPacket(0x02, resp));
      log.debug("[Velocity] Forwarding response sent for {} (version {})", username,
          responseVersion);
    } catch (Exception e) {
      log.error("[Velocity] Failed to build forwarding response: {}", e.getMessage(), e);
      sendPluginFailure(ctx, messageId);
    }
  }

  private void handleEncryptionRequest(ChannelHandlerContext ctx, ByteBuf buf) {
    String serverId = StringUtil.readJavaString(buf);
    byte[] pubKeyBytes = new byte[VarIntUtil.read(buf)];
    buf.readBytes(pubKeyBytes);
    byte[] verifyToken = new byte[VarIntUtil.read(buf)];
    buf.readBytes(verifyToken);

    Thread.ofVirtual().name("mc-auth-join").start(() -> {
      try {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(128);
        SecretKey secretKey = gen.generateKey();

        PublicKey publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(pubKeyBytes));

        JavaAuthManager auth = AuthUtil.getSession();
        String accessToken = auth.getMinecraftToken().getUpToDate().getToken();
        String uuid = auth.getMinecraftProfile().getUpToDate().getId()
            .toString().replace("-", "");

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
        digest.update(secretKey.getEncoded());
        digest.update(pubKeyBytes);
        String hash = new BigInteger(digest.digest()).toString(16);

        com.alibaba.fastjson2.JSONObject payload = new com.alibaba.fastjson2.JSONObject();
        payload.put("accessToken", accessToken);
        payload.put("selectedProfile", uuid);
        payload.put("serverId", hash);

        HttpURLConnection conn = (HttpURLConnection) new URL(
            "https://sessionserver.mojang.com/session/minecraft/join").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(payload.toString().getBytes(StandardCharsets.UTF_8));

        if (conn.getResponseCode() != 204) {
          log.error("Failed to verify session with Mojang (Code: {})", conn.getResponseCode());
          ctx.close();
          return;
        }

        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encSecret = rsa.doFinal(secretKey.getEncoded());
        byte[] encToken = rsa.doFinal(verifyToken);

        ByteBuf resp = ctx.alloc().buffer();
        VarIntUtil.write(resp, encSecret.length);
        resp.writeBytes(encSecret);
        VarIntUtil.write(resp, encToken.length);
        resp.writeBytes(encToken);

        ctx.writeAndFlush(new RawPacket(0x01, resp)).addListener(f -> {
          try {
            Cipher decrypt = Cipher.getInstance("AES/CFB8/NoPadding");
            decrypt.init(Cipher.DECRYPT_MODE, secretKey,
                new IvParameterSpec(secretKey.getEncoded()));
            Cipher encrypt = Cipher.getInstance("AES/CFB8/NoPadding");
            encrypt.init(Cipher.ENCRYPT_MODE, secretKey,
                new IvParameterSpec(secretKey.getEncoded()));

            ctx.pipeline().addFirst("decrypt", new JavaCipherDecoder(decrypt));
            ctx.pipeline().addFirst("encrypt", new JavaCipherEncoder(encrypt));
            log.info("Encryption enabled for Java connection of {}.", cs.getPlayerName());
          } catch (Exception ex) {
            log.error("Failed to install cipher pipeline: {}", ex.getMessage());
            ctx.close();
          }
        });
      } catch (Exception e) {
        log.error("Authentication error for {}: {}", cs.getPlayerName(), e.getMessage(), e);
        ctx.close();
      }
    });
  }

  private String buildBungeeHostname(String realHost, ForwardingMode mode) {
    try {
      String playerIp = ((InetSocketAddress) cs.getConsoleChannel().remoteAddress())
          .getAddress().getHostAddress();
      UUID uuid = resolveUuid();

      JSONArray props = new JSONArray();
      if (mode == ForwardingMode.BUNGEEGUARD) {
        String token = server.getConfig().getForwarding().getBungeeGuardToken();
        if (token != null && !token.isEmpty()) {
          JSONObject tokenProp = new JSONObject();
          tokenProp.put("name", "bungeeguard-token");
          tokenProp.put("value", token);
          props.add(tokenProp);
        }
      }
      return realHost + "\00" + playerIp + "\00" + uuid + "\00" + props.toJSONString();
    } catch (Exception e) {
      log.error("[{}] Failed to build forwarding hostname: {}", mode.name(), e.getMessage(), e);
      return realHost;
    }
  }

  private void sendPluginFailure(ChannelHandlerContext ctx, int messageId) {
    ByteBuf resp = ctx.alloc().buffer();
    VarIntUtil.write(resp, messageId);
    resp.writeBoolean(false);
    ctx.writeAndFlush(new RawPacket(0x02, resp));
  }

  private String resolveLoginName() {
    if (server.getConfig().getAuth().isOnlineMode() && AuthUtil.hasSession()) {
      try {
        return AuthUtil.getSession().getMinecraftProfile().getUpToDate().getName();
      } catch (Exception e) {
        log.error("Failed to retrieve profile name: {}", e.getMessage());
      }
    }
    return server.getConfig().getConnection().getPlayerPrefix() + cs.getPlayerName();
  }

  private UUID resolveUuid() {
    UUID uuid = cs.getUuid();
    if (uuid != null) {
      return uuid;
    }
    return UUID.nameUUIDFromBytes(
        ("OfflinePlayer:" + cs.getPlayerName()).getBytes(StandardCharsets.UTF_8));
  }
}