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

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.config.ServerEntry;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.RawPacket;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.impl.JavaJoinGameS2CPacket;
import dev.briiqn.reunion.core.network.pipeline.java.decode.JavaCompressedPacketDecoder;
import dev.briiqn.reunion.core.network.pipeline.java.decode.JavaViaDecompressor;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaCompressedPacketEncoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaViaCompressor;
import dev.briiqn.reunion.core.network.server.ServerSwitchHandler;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import dev.briiqn.reunion.core.via.ViaManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class JavaChannelHandler extends ChannelInboundHandlerAdapter {

  private final JavaSession session;
  private final ConsoleSession cs;
  private final ReunionServer server;
  private final JavaLoginHandler loginHandler;
  private boolean playPhase = false;

  public JavaChannelHandler(JavaSession session) {
    this.session = session;
    this.cs = session.getConsoleSession();
    this.server = session.getServer();
    this.loginHandler = new JavaLoginHandler(session);
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) {
    session.setJavaChannel(ctx.channel());
    ctx.channel().attr(JavaSession.SESSION_KEY).set(session);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    session.shutdownPacketProcessor();
    log.warn("[Disconnect] Java backend lost for {}. Reason: '{}', Switching: {}",
        cs.getPlayerName(), session.getDisconnectReason(), session.isSwitching());

    Channel consoleChannel = cs.getConsoleChannel();
    if (consoleChannel == null || !consoleChannel.isActive()) {
      return;
    }

    if (cs.getJavaSession() == session && !session.isSwitching()) {
      handleActiveFallback(consoleChannel);
    } else if (cs.getPendingJavaSession() == session) {
      handlePendingFallback(consoleChannel);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof io.netty.handler.codec.DecoderException
        || cause instanceof java.io.IOException
        || cause instanceof IllegalArgumentException
        || cause instanceof IndexOutOfBoundsException) {
      String msg = cause.getMessage();
      if (msg == null || !msg.contains("reset")) {
        log.warn("[Disconnect] JavaSession Netty error for {}: {}",
            cs.getPlayerName(), msg);
      }
    } else {
      log.error("[Disconnect] JavaSession unexpected exception for {}: {}",
          cs.getPlayerName(), cause.getMessage(), cause);
    }
    ctx.close();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (!(msg instanceof RawPacket raw)) {
      return;
    }

    if (!playPhase && raw.id() == 0x03) {
      installCompression(ctx, raw);
    }

    final boolean wasPlay = playPhase;

    if (!playPhase && raw.id() == 0x02) {
      playPhase = true;
    }

    if (wasPlay && (raw.id() == 0x00 || raw.id() == 0x40)) {
      handlePriorityPacket(raw);
      return;
    }

    session.submitPacket(() -> {
      try {
        if (!wasPlay) {
          loginHandler.handle(ctx, raw);
        } else {
          handlePlayPacket(raw);
        }
      } catch (Exception e) {
        log.warn("[Disconnect] JavaSession parse error for {}: {}",
            cs.getPlayerName(), e.getMessage());
        if (log.isDebugEnabled()) {
          log.debug("Stacktrace:", e);
        }
        ctx.close();
      } finally {
        raw.payload().release();
      }
    });
  }

  private void installCompression(ChannelHandlerContext ctx, RawPacket raw) {
    int threshold = VarIntUtil.read(raw.payload().duplicate());
    if (ViaManager.isEnabled()) {
      if (ctx.pipeline().get("decompressor") == null) {
        ctx.pipeline().addBefore("via-codec", "decompressor", new JavaViaDecompressor(threshold));

      }
      if (ctx.pipeline().get("compressor") == null) {
        ctx.pipeline().addAfter("length-encoder", "compressor", new JavaViaCompressor(threshold));

      }
    } else {
      ctx.pipeline().replace("decoder", "decoder", new JavaCompressedPacketDecoder(threshold));
      ctx.pipeline().replace("encoder", "encoder", new JavaCompressedPacketEncoder(threshold));
    }
  }

  private void handlePriorityPacket(RawPacket raw) {
    try {
      Packet pkt = server.getPacketRegistry().create(PacketSide.JAVA_S2C, raw.id());
      if (pkt != null) {
        pkt.read(raw.payload().duplicate(), JavaSession.JAVA_PROTOCOL);
        ((JavaS2CPacket) pkt).handle(session, JavaSession.JAVA_PROTOCOL);
      }
    } catch (Exception e) {
      log.warn("[JavaSession] Parse error on priority packet 0x{}: {}",
          Integer.toHexString(raw.id()), e.getMessage());
    } finally {
      raw.payload().release();
    }
  }

  private void handlePlayPacket(RawPacket raw) {
    if (!cs.isLoggedIn() && raw.id() == 0x01) {
      Packet pkt = new JavaJoinGameS2CPacket();
      pkt.read(raw.payload().duplicate(), JavaSession.JAVA_PROTOCOL);
      ((JavaS2CPacket) pkt).handle(session, JavaSession.JAVA_PROTOCOL);
      return;
    }

    if (raw.id() == 0x21) {
      cs.getChunkManager().handleChunkData(raw.payload());
      return;
    }
    if (raw.id() == 0x26) {
      cs.getChunkManager().handleMapChunkBulk(raw.payload());
      return;
    }

    Packet pkt = server.getPacketRegistry().create(PacketSide.JAVA_S2C, raw.id());
    if (pkt == null) {
      return;
    }
    pkt.read(raw.payload().duplicate(), JavaSession.JAVA_PROTOCOL);
    ((JavaS2CPacket) pkt).handle(session, JavaSession.JAVA_PROTOCOL);
  }


  private void handleActiveFallback(Channel consoleChannel) {
    if (!server.getConfig().getNetwork().isNetworkMode()) {
      PacketManager.sendToConsole(cs, new ConsoleDisconnectS2CPacket());
      consoleChannel.close();
      return;
    }

    if (cs.isFallingBack()) {
      log.warn("[Disconnect] {} was already falling back. Closing console.", cs.getPlayerName());
      PacketManager.sendToConsole(cs, new ConsoleDisconnectS2CPacket());
      consoleChannel.close();
      return;
    }

    String current = cs.getCurrentServer();
    String prev = cs.getPreviousServerName();
    ServerEntry fallback = null;

    if (prev != null && !prev.equalsIgnoreCase(current)) {
      fallback = server.getConfig().getServer(prev);
    }
    if (fallback == null) {
      fallback = server.getConfig().getDefaultServer();
    }

    if (fallback == null || fallback.name().equalsIgnoreCase(current)) {
      log.warn("[Disconnect] No fallback for {}. Closing console.", cs.getPlayerName());
      PacketManager.sendToConsole(cs, new ConsoleDisconnectS2CPacket());
      consoleChannel.close();
      return;
    }

    cs.setFallingBack(true);
    cs.queueOrSendChat("Kicked from " + current + ": " + session.getDisconnectReason());
    ServerSwitchHandler.switchServer(cs, fallback);
  }

  private void handlePendingFallback(Channel consoleChannel) {
    log.warn("[Disconnect] Pending Java connection failed for {}. Reason: {}",
        cs.getPlayerName(), session.getDisconnectReason());
    cs.setPendingJavaSession(null);
    cs.setPendingServerName(null);
    cs.queueOrSendChat("Failed to connect: " + session.getDisconnectReason());

    if (cs.isFallingBack()) {
      PacketManager.sendToConsole(cs, new ConsoleDisconnectS2CPacket());
      consoleChannel.close();
    }
  }
}