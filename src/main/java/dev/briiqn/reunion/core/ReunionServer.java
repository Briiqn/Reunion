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

package dev.briiqn.reunion.core;

import dev.briiqn.reunion.api.ReunionProxyImpl;
import dev.briiqn.reunion.core.config.Config;
import dev.briiqn.reunion.core.console.Console;
import dev.briiqn.reunion.core.data.Geometry;
import dev.briiqn.reunion.core.manager.BanManager;
import dev.briiqn.reunion.core.manager.CommandManager;
import dev.briiqn.reunion.core.network.lan.impl.LCEMPLANBroadcaster;
import dev.briiqn.reunion.core.network.packet.data.enums.DisconnectReason;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleDisconnectS2CPacket;
import dev.briiqn.reunion.core.network.packet.registry.PacketRegistry;
import dev.briiqn.reunion.core.network.pipeline.console.decode.ConsoleFrameDecoder;
import dev.briiqn.reunion.core.network.pipeline.console.decode.ConsolePacketDecoder;
import dev.briiqn.reunion.core.network.pipeline.console.encode.ConsolePacketEncoder;
import dev.briiqn.reunion.core.plugin.ReunionServerBridge;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.registry.impl.BlockRegistry;
import dev.briiqn.reunion.core.registry.impl.EntityRegistry;
import dev.briiqn.reunion.core.registry.impl.ItemRegistry;
import dev.briiqn.reunion.core.registry.impl.LanguageRegistry;
import dev.briiqn.reunion.core.registry.impl.RecipeRegistry;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.auth.AuthUtil;
import dev.briiqn.reunion.core.via.ViaManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Getter
@Log4j2
public final class ReunionServer {

  private static final ByteBufAllocator DIRECT_ALLOC = new PooledByteBufAllocator(true);
  public static String releaseVersion = "1.0.0";
  @Getter(AccessLevel.NONE)
  public static int maxSupportedClientProtocol = 78;

  private final Config config;
  private final BanManager banManager;
  private final CommandManager commandManager;
  @Getter(AccessLevel.NONE)
  private final AtomicInteger smallIdAlloc = new AtomicInteger(1);
  private final PacketRegistry packetRegistry;
  private final Map<String, ConsoleSession> sessions = new ConcurrentHashMap<>();
  @Getter(AccessLevel.NONE)
  private final Map<String, byte[]> lceTextureCache = new ConcurrentHashMap<>();
  @Getter(AccessLevel.NONE)
  private final Set<String> lceRejectedTextures = ConcurrentHashMap.newKeySet();
  @Getter(AccessLevel.NONE)
  private final Map<String, Geometry> lceGeometryCache = new ConcurrentHashMap<>();
  @Getter(AccessLevel.NONE)
  private final Map<String, ConsoleSession> lceTextureOwner = new ConcurrentHashMap<>();
  @Getter(AccessLevel.NONE)
  private final Map<String, Set<ConsoleSession>> lceTexturePending = new ConcurrentHashMap<>();

  public Console console;
  @Getter(AccessLevel.NONE)
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  @Getter(AccessLevel.NONE)
  private Channel tcpServerChannel;
  @Getter(AccessLevel.NONE)
  private LCEMPLANBroadcaster lanBroadcaster;
  private ReunionProxyImpl pluginApi;

  public ReunionServer() {
    this.config = new Config(this);
    this.banManager = new BanManager();
    this.packetRegistry = new PacketRegistry();
    this.commandManager = new CommandManager(this);
    this.console = new Console(this);

    System.out.println(" 888888ba                             oo                   \n" +
        " 88    `8b                                                 \n" +
        "a88aaaa8P' .d8888b. dP    dP 88d888b. dP .d8888b. 88d888b. \n" +
        " 88   `8b. 88ooood8 88    88 88'  `88 88 88'  `88 88'  `88 \n" +
        " 88     88 88.  ... 88.  .88 88    88 88 88.  .88 88    88 \n" +
        " dP     dP `88888P' `88888P' dP    dP dP `88888P' dP    dP \n" +
        "by Briiqn                                                    \n" +
        "______________________________________________________________");
    initRegistries();
  }

  public void start() throws Exception {
    packetRegistry.scan("dev.briiqn.reunion");

    ViaManager.init(config);

    ReunionServerBridge bridge = new ReunionServerBridge(this);
    pluginApi = new ReunionProxyImpl(bridge, Path.of("plugins"));
    PluginEventHooks.init(pluginApi);
    pluginApi.start();

    boolean useEpoll = Epoll.isAvailable();
    if (config.getAuth().isOnlineMode()) {
      log.info("Getting Session");
      Thread.ofVirtual().start(() -> {
        try {
          AuthUtil.getSession();
          log.info(
              "Logged in as " + AuthUtil.getSession().getMinecraftProfile().getCached().getName());
        } catch (Exception e) {
          log.error("Failed to auth: {}", e.getMessage());
        }
      });
    }

    bossGroup = new MultiThreadIoEventLoopGroup(1,
        useEpoll ? EpollIoHandler.newFactory() : NioIoHandler.newFactory());

    workerGroup = new MultiThreadIoEventLoopGroup(config.getNetwork().getNettyThreads(),
        useEpoll ? EpollIoHandler.newFactory() : NioIoHandler.newFactory());

    ServerBootstrap sb = new ServerBootstrap();
    sb.group(bossGroup, workerGroup)
        .channel(useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
        .option(ChannelOption.ALLOCATOR, DIRECT_ALLOC)
        .childOption(ChannelOption.ALLOCATOR, DIRECT_ALLOC)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
            new WriteBufferWaterMark(32 * 1024, 64 * 1024))
        .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            byte sid = (byte) smallIdAlloc.getAndIncrement();
            ConsoleSession session = new ConsoleSession(
                ch, config.getConnection().getJavaHost(), config.getConnection().getJavaPort(), sid,
                ReunionServer.this);
            ch.pipeline()
                .addLast("frame", new ConsoleFrameDecoder())
                .addLast("decoder", new ConsolePacketDecoder())
                .addLast("encoder", new ConsolePacketEncoder())
                .addLast("handler", session.consoleChannelHandler());
          }
        });

    int port = config.getConnection().getListenPort();
    tcpServerChannel = sb.bind(port).sync().channel();

    lanBroadcaster = new LCEMPLANBroadcaster(workerGroup, config.getConnection().getJavaHost(),
        port);
    lanBroadcaster.start();

    log.info("Listening on :" + port);
    new Thread(console::start, "reunion-console").start();
  }

  public void stop() {
    for (ConsoleSession session : sessions.values()) {
      if (session.getChunkManager() != null) {
        session.getChunkManager().clearCache();
      }
      PacketManager.sendToConsole(session,
          new ConsoleDisconnectS2CPacket(DisconnectReason.EXITED_GAME));
    }
    if (pluginApi != null) {
      pluginApi.stop();
    }
    if (lanBroadcaster != null) {
      lanBroadcaster.stop();
    }
    if (tcpServerChannel != null) {
      tcpServerChannel.close();
    }
    if (bossGroup != null) {
      bossGroup.shutdownGracefully();
    }
    if (workerGroup != null) {
      workerGroup.shutdownGracefully();
    }
    config.save();
    log.info("Server stopped.");
  }

  private void initRegistries() {
    BlockRegistry.getInstance();
    ItemRegistry.getInstance();
    EntityRegistry.getInstance();
    LanguageRegistry.getInstance();
    RecipeRegistry.getInstance();
  }

  public void addSession(String name, ConsoleSession session) {
    sessions.put(name.toLowerCase(), session);
  }

  public void removeSession(String name) {
    if (name != null) {
      sessions.remove(name.toLowerCase());
    }
  }

  public ConsoleSession getSession(String name) {
    return sessions.get(name.toLowerCase());
  }

  public String getJavaHost() {
    return config.getConnection().getJavaHost();
  }

  public int getJavaPort() {
    return config.getConnection().getJavaPort();
  }

  public void registerTextureOwner(String textureName, ConsoleSession owner) {
    lceTextureOwner.put(textureName, owner);
  }

  public ConsoleSession getLceTextureOwner(String textureName) {
    return lceTextureOwner.get(textureName);
  }


  public byte[] getCachedLceTexture(String textureName) {
    return lceTextureCache.get(textureName);
  }

  public Set<ConsoleSession> deliverLceTexture(String textureName, byte[] data) {
    lceTextureCache.put(textureName, data);
    Set<ConsoleSession> waiters = lceTexturePending.remove(textureName);
    return waiters != null ? waiters : Set.of();
  }

  public Geometry getCachedGeometry(String textureName) {
    return lceGeometryCache.get(textureName);
  }

  public Set<ConsoleSession> deliverGeometry(String textureName, Geometry geo) {
    lceGeometryCache.put(textureName, geo);
    lceTextureCache.put(textureName, geo.textureData());
    Set<ConsoleSession> waiters = lceTexturePending.remove(textureName);
    return waiters != null ? waiters : Set.of();
  }

  public boolean addLceTexturePending(String textureName, ConsoleSession waiter) {
    Set<ConsoleSession> set = lceTexturePending
        .computeIfAbsent(textureName, k -> ConcurrentHashMap.newKeySet());
    boolean wasEmpty = set.isEmpty();
    set.add(waiter);
    return wasEmpty;
  }

  public void removeLceTextureOwner(ConsoleSession session) {
    lceTextureOwner.values().removeIf(s -> s == session);
    lceTexturePending.values().forEach(set -> set.remove(session));
  }

  public void evictLceTexture(String textureName) {
    if (textureName == null) {
      return;
    }
    lceTextureCache.remove(textureName);
    lceGeometryCache.remove(textureName);
    lceTextureOwner.remove(textureName);
    lceRejectedTextures.add(textureName);
    log.info("[LCE Cache] Evicted and blacklisted invalid texture/geometry: '{}'", textureName);
  }

  public boolean isLceTextureRejected(String textureName) {
    return textureName != null && lceRejectedTextures.contains(textureName);
  }

}