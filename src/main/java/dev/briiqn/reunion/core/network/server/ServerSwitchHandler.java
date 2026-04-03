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
package dev.briiqn.reunion.core.network.server;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.platform.ViaCodecHandler;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import dev.briiqn.reunion.api.event.player.ServerSwitchEvent;
import dev.briiqn.reunion.core.config.ServerEntry;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.pipeline.java.decode.JavaPacketDecoder;
import dev.briiqn.reunion.core.network.pipeline.java.decode.VarIntFrameDecoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaLengthPrefixEncoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaPacketEncoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaRawPacketSerializer;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.game.ServerPinger;
import dev.briiqn.reunion.core.via.ViaManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerSwitchHandler {

  public static final AttributeKey<ProtocolVersion> BACKEND_VERSION =
      AttributeKey.valueOf("backend-version");

  public static void switchServer(ConsoleSession session, ServerEntry target) {
    if (session.getConsoleChannel() == null || !session.getConsoleChannel().isActive()) {
      return;
    }

    String previousServer = session.getCurrentServer() != null ? session.getCurrentServer() : "";

    if (target.name().equalsIgnoreCase(previousServer)) {
      session.queueOrSendChat("[Reunion] You are already connected to this server.");
      return;
    }

    if (session.getPendingJavaSession() != null && target.name()
        .equalsIgnoreCase(session.getPendingServerName())) {
      session.queueOrSendChat(
          "[Reunion] Already connecting to " + target.name() + "...");
      return;
    }

    ServerSwitchEvent event = PluginEventHooks.fireServerSwitch(
        new ConsoleSessionPlayerAdapter(session), previousServer, target.name());

    if (event.isCancelled()) {
      return;
    }

    ProtocolVersion backendVersion = null;
    if (ViaManager.isEnabled()) {
      backendVersion = ServerPinger.detect(target.host(), target.port());
      if (backendVersion == null) {
        log.warn("[ServerSwitch] Could not detect version for {}, Via will use default",
            target.name());
      } else {
        log.info("[ServerSwitch] Detected {} → {}", target.name(), backendVersion.getName());
      }
    }

    final ProtocolVersion resolvedVersion = backendVersion;

    session.queueOrSendChat(
        "[Reunion] Connecting to " + target.name() + "...");

    Bootstrap b = new Bootstrap();
    b.group(session.getConsoleChannel().eventLoop())
        .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            if (resolvedVersion != null) {
              ch.attr(BACKEND_VERSION).set(resolvedVersion);
            }
            JavaSession javaSession = new JavaSession(session, session.getServer());
            if (ViaManager.isEnabled()) {
              UserConnection conn = new UserConnectionImpl(ch, true);
              new ProtocolPipelineImpl(conn);
              ch.pipeline()
                  .addLast("frame", new VarIntFrameDecoder())
                  .addLast("length-encoder", new JavaLengthPrefixEncoder())
                  .addLast("via-codec", new ViaCodecHandler(conn))
                  .addLast("decoder", new JavaPacketDecoder())
                  .addLast("encoder", new JavaRawPacketSerializer())
                  .addLast("handler", javaSession.javaChannelHandler());
            } else {
              ch.pipeline()
                  .addLast("frame", new VarIntFrameDecoder())
                  .addLast("decoder", new JavaPacketDecoder())
                  .addLast("encoder", new JavaPacketEncoder())
                  .addLast("handler", javaSession.javaChannelHandler());
            }
          }
        });

    b.connect(target.host(), target.port()).addListener((ChannelFuture f) -> {
      if (f.isSuccess()) {
        if (session.getConsoleChannel() == null || !session.getConsoleChannel().isActive()) {
          f.channel().close();
          return;
        }
        JavaSession pending = f.channel().attr(JavaSession.SESSION_KEY).get();
        session.setPendingJavaSession(pending);
        session.setPendingServerName(target.name());
        pending.sendHandshake(session.getPlayerName(), target.host(), target.port());
      } else {
        log.error("[ServerSwitch] Failed to connect {} to {}",
            session.getPlayerName(), target.name());
        session.queueOrSendChat(
            "[Reunion] Failed to connect to " + target.name() + ".");
        session.setPendingJavaSession(null);
        session.setPendingServerName(null);
        if (session.isFallingBack()) {
          PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
          session.getConsoleChannel().close();
        }
      }
    });
  }
}