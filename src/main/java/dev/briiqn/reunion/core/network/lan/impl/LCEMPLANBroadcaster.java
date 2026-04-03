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

package dev.briiqn.reunion.core.network.lan.impl;

import dev.briiqn.reunion.core.network.lan.BroadcastMode;
import dev.briiqn.reunion.core.network.lan.LANBroadcaster;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class LCEMPLANBroadcaster extends LANBroadcaster {

  private static final int LAN_UDP_PORT = 25566;
  private static final int LAN_BROADCAST_MAGIC = 0x4D434C4E;

  private final EventLoopGroup workerGroup;
  private final String javaHost;
  private final int listenPort;

  private Channel listenChannel;
  private Channel broadcastChannel;

  public LCEMPLANBroadcaster(EventLoopGroup workerGroup, String javaHost, int listenPort) {
    super(BroadcastMode.UDP);
    this.workerGroup = workerGroup;
    this.javaHost = javaHost;
    this.listenPort = listenPort;
  }

  @Override
  public void start() throws Exception {
    Bootstrap udpIn = new Bootstrap();
    udpIn.group(workerGroup)
        .channel(epollAvailable() ? EpollDatagramChannel.class : NioDatagramChannel.class)
        .option(ChannelOption.SO_REUSEADDR, true)
        .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
          @Override
          protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket pkt) {
          }
        });
    listenChannel = udpIn.bind(LAN_UDP_PORT).sync().channel();

    Bootstrap udpOut = new Bootstrap();
    udpOut.group(workerGroup)
        .channel(epollAvailable() ? EpollDatagramChannel.class : NioDatagramChannel.class)
        .option(ChannelOption.SO_BROADCAST, true)
        .handler(new ChannelInboundHandlerAdapter());
    broadcastChannel = udpOut.bind(0).sync().channel();

    workerGroup.scheduleAtFixedRate(this::broadcast, 0, 1, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (listenChannel != null) {
      listenChannel.close();
    }
    if (broadcastChannel != null) {
      broadcastChannel.close();
    }
  }

  @Override
  public void broadcast() {
    try {
      ByteBuf buf = buildBroadcastPacket(broadcastChannel.alloc());
      broadcastChannel.writeAndFlush(
          new DatagramPacket(buf, new InetSocketAddress("255.255.255.255", LAN_UDP_PORT)));
    } catch (Exception e) {
      e.printStackTrace();
      log.error("[LAN] broadcast error: " + e.getMessage());
    }
  }

  private ByteBuf buildBroadcastPacket(io.netty.buffer.ByteBufAllocator alloc) {
    ByteBuf b = alloc.buffer(84);
    b.writeIntLE(LAN_BROADCAST_MAGIC);
    b.writeShortLE(1);
    b.writeShortLE(listenPort);
    byte[] name = (javaHost).getBytes(StandardCharsets.UTF_16LE);
    byte[] pad = new byte[64];
    System.arraycopy(name, 0, pad, 0, Math.min(name.length, 64));
    b.writeBytes(pad);
    b.writeByte(1);
    b.writeByte(8);
    b.writeIntLE(0);
    b.writeIntLE(0);
    b.writeByte(0);
    b.writeByte(1);
    return b;
  }


}
