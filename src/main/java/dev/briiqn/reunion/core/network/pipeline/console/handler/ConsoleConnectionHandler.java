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

package dev.briiqn.reunion.core.network.pipeline.console.handler;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.manager.ChunkManager;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.RawPacket;
import dev.briiqn.reunion.core.network.packet.data.enums.DisconnectReason;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ConsoleConnectionHandler extends ChannelInboundHandlerAdapter {

  private final ConsoleSession session;
  private final ReunionServer server;

  public ConsoleConnectionHandler(ConsoleSession session) {
    this.session = session;
    this.server = session.getServer();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    ctx.writeAndFlush(ctx.alloc().buffer(1).writeByte(session.getSmallId() & 0xFF));
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    log.warn("[Disconnect] Console connection lost for player {} from {}.",
        session.getPlayerName(), ctx.channel().remoteAddress());

    PluginEventHooks.fireDisconnect(
        new ConsoleSessionPlayerAdapter(session), "disconnected");

    server.removeLceTextureOwner(session);

    JavaSession active = session.getJavaSession();
    JavaSession pending = session.getPendingJavaSession();
    if (active != null) {
      active.close();
    }
    if (pending != null) {
      pending.close();
    }

    ChunkManager cm = session.getChunkManager();
    if (cm != null) {
      cm.clearCache();
    }

    server.removeSession(session.getPlayerName());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    String msg = cause.getMessage();
    if (cause instanceof io.netty.handler.codec.DecoderException
        || cause instanceof java.io.IOException
        || cause instanceof IllegalArgumentException
        || cause instanceof IndexOutOfBoundsException) {
      if (msg == null || !msg.contains("reset")) {
        log.warn("[Disconnect] Console Network/Decode error from {}: {}",
            ctx.channel().remoteAddress(), msg);
      }
    } else {
      log.error("[Disconnect] Console unexpected error from {}: {}",
          ctx.channel().remoteAddress(), msg, cause);
    }
    ctx.close();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof RawPacket raw)) {
      return;
    }

    ByteBuf data = raw.payload();
    try {
      while (data.isReadable()) {
        int id = data.readUnsignedByte();
        Packet pkt = server.getPacketRegistry()
            .create(PacketSide.CONSOLE_C2S, id);

        if (pkt == null) {
          log.warn("[Disconnect] Unknown Console Packet: 0x{} from {}. Kicking.",
              Integer.toHexString(id), ctx.channel().remoteAddress());
          PacketManager.sendToConsole(session,
              new ConsoleDisconnectS2CPacket(DisconnectReason.UNEXPECTED_PACKET));
          ctx.close();
          break;
        }

        pkt.read(data, session.getClientVersion());
        ((ConsoleC2SPacket) pkt).handle(session, session.getClientVersion());
      }
    } catch (Exception e) {
      log.warn("[Disconnect] Console parse error from {}: {}",
          ctx.channel().remoteAddress(), e.getMessage());
      if (log.isDebugEnabled()) {
        log.debug("Stacktrace:", e);
      }

      PacketManager.sendToConsole(session,
          new ConsoleDisconnectS2CPacket(DisconnectReason.UNEXPECTED_PACKET));
      ctx.close();
    } finally {
      data.release();
    }
  }
}