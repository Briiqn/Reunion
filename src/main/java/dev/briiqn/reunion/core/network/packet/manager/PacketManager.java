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

package dev.briiqn.reunion.core.network.packet.manager;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.data.RawPacket;
import dev.briiqn.reunion.core.network.packet.registry.PacketRegistry;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class PacketManager {

  private PacketManager() {
  }

  public static void sendToConsole(ConsoleSession session, Packet packet) {
    Channel ch = session.getConsoleChannel();
    if (ch == null || !ch.isActive()) {
      return;
    }

    PacketInfo info = PacketRegistry.getPacketInfo(packet.getClass());
    ByteBuf preview = ch.alloc().buffer();

    try {
      packet.write(preview, session.getClientVersion());

      StringBuilder hex = new StringBuilder();

      if (log.isDebugEnabled()) {
        for (int i = 0; i < preview.readableBytes(); i++) {
          hex.append(String.format("%02X ", preview.getByte(i)));
        }
        log.debug("[Console] Sending: {} (id=0x{}) bytes: {}",
            packet.getClass().getSimpleName(),
            info != null ? String.format("%02X", info.id()) : "??",
            hex.toString().trim());
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      preview.release();
    }

    send(ch, packet, session.getClientVersion());
  }


  public static void sendToJava(JavaSession session, Packet packet) {
    if (session == null) {
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug("[Java] Sending: " + packet.getClass().getSimpleName());
    }

    Channel ch = session.getJavaChannel();
    if (ch == null || !ch.isActive()) {
      return;
    }
    send(ch, packet, 47);
  }

  public static void send(Channel ch, Packet packet, int protocol) {
    PacketInfo info = PacketRegistry.getPacketInfo(packet.getClass());
    if (info == null) {
      throw new IllegalArgumentException(
          "Packet lacks @PacketInfo: " + packet.getClass().getSimpleName());
    }

    ByteBuf payload = ch.alloc().buffer();
    try {
      packet.write(payload, protocol);
      ch.writeAndFlush(new RawPacket(info.id(), payload));
    } catch (Exception e) {
      payload.release();
      log.error("Failed to encode packet " + packet.getClass().getSimpleName(), e);
    }
  }
}