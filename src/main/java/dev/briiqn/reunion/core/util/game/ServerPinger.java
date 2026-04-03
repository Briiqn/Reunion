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
package dev.briiqn.reunion.core.util.game;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ServerPinger {

  private static final int TIMEOUT_MS = 5_000;
  private static final Map<String, ProtocolVersion> CACHE = new ConcurrentHashMap<>();

  private ServerPinger() {
  }

  public static ProtocolVersion detect(String host, int port) {
    String key = host.toLowerCase() + ":" + port;
    ProtocolVersion cached = CACHE.get(key);
    if (cached != null) {
      return cached;
    }

    try (Socket socket = new Socket()) {
      socket.setTcpNoDelay(true);
      socket.setSoTimeout(TIMEOUT_MS);
      socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);

      OutputStream out = socket.getOutputStream();
      DataInputStream in = new DataInputStream(socket.getInputStream());

      ByteBuf handshake = Unpooled.buffer();
      VarIntUtil.write(handshake, 0x00);
      VarIntUtil.write(handshake, ProtocolVersion.v26_1.getVersion());
      writeString(handshake, host);
      handshake.writeShort(port);
      VarIntUtil.write(handshake, 1);
      writeFramedPacket(out, handshake);

      ByteBuf statusRequest = Unpooled.buffer();
      VarIntUtil.write(statusRequest, 0x00);
      writeFramedPacket(out, statusRequest);
      out.flush();

      int size = read(in);
      byte[] data = new byte[size];
      in.readFully(data);

      ByteBuf responseBuf = Unpooled.wrappedBuffer(data);
      int packetId = VarIntUtil.read(responseBuf);
      if (packetId != 0x00) {
        throw new IOException("Unexpected packet ID: " + packetId);
      }

      JSONObject root = JSON.parseObject(readString(responseBuf));
      JSONObject version = root.getJSONObject("version");
      if (version == null) {
        throw new IOException("Missing version field");
      }

      int protocol = version.getIntValue("protocol");
      ProtocolVersion pv = ProtocolVersion.getProtocol(protocol);
      if (pv == null || pv == ProtocolVersion.unknown) {
        log.warn("[ServerPinger] Unknown protocol {} from {}:{}", protocol, host, port);
        return null;
      }

      log.debug("[ServerPinger] {}:{} reported {}", host, port, pv.getName());
      CACHE.put(key, pv);
      return pv;

    } catch (IOException e) {
      log.warn("[ServerPinger] Failed to ping {}:{}  {}", host, port, e.getMessage());
      return null;
    }
  }

  public static int ping(String host, int port) {
    ProtocolVersion pv = detect(host, port);
    return pv != null ? pv.getVersion() : -1;
  }

  public static void invalidate(String host, int port) {
    CACHE.remove(host.toLowerCase() + ":" + port);
  }

  public static void clear() {
    CACHE.clear();
  }

  private static void writeFramedPacket(OutputStream out, ByteBuf packet) throws IOException {
    ByteBuf frame = Unpooled.buffer();
    try {
      VarIntUtil.write(frame, packet.readableBytes());
      byte[] lengthBytes = new byte[frame.readableBytes()];
      frame.readBytes(lengthBytes);
      out.write(lengthBytes);

      byte[] data = new byte[packet.readableBytes()];
      packet.readBytes(data);
      out.write(data);
    } finally {
      frame.release();
      packet.release();
    }
  }

  private static int read(DataInputStream in) throws IOException {
    ByteBuf buf = Unpooled.buffer(5);
    try {
      byte b;
      do {
        b = in.readByte();
        buf.writeByte(b);
      } while ((b & 0x80) != 0);
      return VarIntUtil.read(buf);
    } finally {
      buf.release();
    }
  }

  private static void writeString(ByteBuf buf, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    VarIntUtil.write(buf, bytes.length);
    buf.writeBytes(bytes);
  }

  private static String readString(ByteBuf buf) {
    int len = VarIntUtil.read(buf);
    byte[] bytes = new byte[len];
    buf.readBytes(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}