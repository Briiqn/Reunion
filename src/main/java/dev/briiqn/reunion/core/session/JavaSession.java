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

package dev.briiqn.reunion.core.session;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.registry.PacketRegistry;
import dev.briiqn.reunion.core.network.pipeline.java.handler.JavaChannelHandler;
import dev.briiqn.reunion.core.network.pipeline.java.handler.JavaLoginHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@RequiredArgsConstructor
@Log4j2
public final class JavaSession {

  public static final AttributeKey<JavaSession> SESSION_KEY =
      AttributeKey.valueOf("reunion.java_session");

  public static final int JAVA_PROTOCOL = 47;

  private final ConsoleSession consoleSession;

  @Getter(AccessLevel.PUBLIC)
  private final ReunionServer server;
  private final ExecutorService packetProcessor =
      Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
  @Getter(AccessLevel.PUBLIC)
  @Setter(AccessLevel.PUBLIC)
  private Channel javaChannel;
  @Getter
  private boolean switching = false;
  @Getter
  @Setter(AccessLevel.PUBLIC)
  private String disconnectReason = "Server closed";

  public ChannelInboundHandlerAdapter javaChannelHandler() {
    return new JavaChannelHandler(this);
  }

  public void sendHandshake(String playerName, String host, int port) {
    new JavaLoginHandler(this).sendHandshake(host, port);
  }

  public void submitPacket(Runnable task) {
    packetProcessor.execute(task);
  }

  public void shutdownPacketProcessor() {
    packetProcessor.shutdown();
  }

  public void close(boolean isSwitch) {
    this.switching = isSwitch;
    if (javaChannel != null && javaChannel.isActive()) {
      javaChannel.close();
    }
  }

  public void close() {
    close(false);
  }

  public PacketSide getPacketSide(Packet packet) {
    PacketInfo info = PacketRegistry.getPacketInfo(packet.getClass());
    return info != null ? info.side() : PacketSide.JAVA_C2S;
  }
}