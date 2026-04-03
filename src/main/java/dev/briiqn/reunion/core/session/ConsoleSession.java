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
import dev.briiqn.reunion.core.manager.ChunkManager;
import dev.briiqn.reunion.core.manager.EntityManager;
import dev.briiqn.reunion.core.manager.PlayerManager;
import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.data.enums.GamePrivilege;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleChatS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleContainerCloseS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleLoginS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsolePreLoginS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleRemoveEntitiesS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleRespawnS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaClickWindowC2SPacket;
import dev.briiqn.reunion.core.network.packet.registry.PacketRegistry;
import dev.briiqn.reunion.core.network.pipeline.console.handler.ConsoleConnectionHandler;
import dev.briiqn.reunion.core.network.pipeline.console.handler.ConsoleWorldHackHandler;
import dev.briiqn.reunion.core.network.server.ServerConnector;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.util.container.CraftingTranslator;
import dev.briiqn.reunion.core.util.container.InventoryTracker;
import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public final class ConsoleSession {

  private final byte smallId;
  private final ReunionServer server;

  private final ConsoleWorldHackHandler worldHack = new ConsoleWorldHackHandler(this);
  private final EntityManager entityManager = new EntityManager();
  private final PlayerManager playerManager = new PlayerManager();
  private final InventoryTracker inventoryTracker = new InventoryTracker();
  private final CraftingTranslator craftingTranslator = new CraftingTranslator(this);

  @Getter(AccessLevel.NONE)
  private final Int2IntMap windowTypes = Int2IntMaps.synchronize(new Int2IntOpenHashMap());
  @Getter(AccessLevel.NONE)
  private final Queue<Vec3d> pendingTeleports = new ArrayDeque<>();
  @Getter(AccessLevel.NONE)
  private final Queue<String> pendingChatQueue = new ArrayDeque<>();
  @Getter(AccessLevel.NONE)
  private final List<Runnable> tickActionQueue = new ArrayList<>();
  private final AtomicInteger pendingTeleportAcks = new AtomicInteger(0);
  private final Channel consoleChannel;
  @Setter
  int playerPrivileges = GamePrivilege.grant(
      GamePrivilege.TRUST_PLAYERS,
      GamePrivilege.BUILD_AND_MINE,
      GamePrivilege.USE_DOORS_AND_SWITCHES,
      GamePrivilege.OPEN_CONTAINERS,
      GamePrivilege.ATTACK_PLAYERS,
      GamePrivilege.ATTACK_MOBS
  );
  @Setter
  int worldOffsetX = 0;
  @Setter
  int worldOffsetZ = 0;
  @Getter(AccessLevel.NONE)
  private short actionCounter = 1;
  @Getter(AccessLevel.NONE)
  private volatile long keepAliveSentAt = -1;
  @Setter
  private JavaSession javaSession;
  @Setter
  private JavaSession pendingJavaSession;
  @Setter
  private String pendingServerName;
  @Setter
  private String previousServerName;
  @Setter
  private volatile boolean fallingBack = false;
  @Setter
  private int clientVersion = 78;
  @Setter
  private String playerName = "Player";
  @Setter
  private long xuid = 0L;
  @Setter
  private boolean loggedIn = false;
  private int javaEntityId = -1;
  @Setter
  private int safePlayerId = -1;

  @Setter
  private int ridingEntityId = -1;
  @Setter
  private byte tradeWindowId = -1;
  @Setter
  private int dimension = 0;
  @Setter
  private short gamemode = 0;
  @Setter
  private short difficulty = 1;
  @Setter
  private String levelType = "DEFAULT";
  @Setter
  private Vec3d lastPos = Vec3d.ZERO;
  @Setter
  private Vec2f lastRot = Vec2f.ZERO;

  private ChunkManager chunkManager;

  @Setter
  private boolean usingItem = false;
  @Setter
  private Object lastUsedItem = null;
  @Setter
  private boolean needsReblock = false;

  private volatile int ping = -1;
  private volatile String currentServer = null;

  @Setter
  private volatile UUID uuid = null;
  @Setter
  private float health = 20f;
  @Setter
  private int foodLevel = 20;
  @Setter
  private boolean sneaking = false;
  @Setter
  private boolean sprinting = false;
  @Setter
  private int heldItemSlot = 0;
  @Setter
  private long gameTime = 0L;
  @Setter
  private long dayTime = 0L;

  private long ticksAlive = 0L;

  @Setter
  private volatile int lceSkinDwId = 0;
  @Setter
  private volatile String lceSkinName = null;
  @Setter
  private volatile String lceCapeName = null;
  @Setter
  private volatile int backendProtocol = 47;
  @Setter
  private boolean windowTransitioning = false;
  @Setter
  private boolean waitingForInitialTeleport = false;
  @Setter
  private int furnaceBurnTotal;

  private boolean clientScreenReady = false;

  public ConsoleSession(Channel consoleChannel, String javaHost, int javaPort,
      byte smallId, ReunionServer server) {
    this.consoleChannel = consoleChannel;
    this.smallId = smallId;
    this.server = server;
  }

  public double getLastX() {
    return lastPos.x();
  }

  public double getLastY() {
    return lastPos.y();
  }

  public double getLastZ() {
    return lastPos.z();
  }

  public float getLastYaw() {
    return lastRot.yaw();
  }

  public float getLastPitch() {
    return lastRot.pitch();
  }

  public ChannelInboundHandlerAdapter consoleChannelHandler() {
    return new ConsoleConnectionHandler(this);
  }

  public void markClientScreenReady() {
    if (clientScreenReady) {
      return;
    }
    clientScreenReady = true;
    flushPendingChat();
  }

  public void queueOrSendChat(String message) {
    if (clientScreenReady) {
      PacketManager.sendToConsole(this, new ConsoleChatS2CPacket(message));
    } else {
      pendingChatQueue.add(message);
    }
  }

  private void flushPendingChat() {
    String msg;
    while ((msg = pendingChatQueue.poll()) != null) {
      PacketManager.sendToConsole(this, new ConsoleChatS2CPacket(msg));
    }
  }

  public void onKeepAliveSent() {
    keepAliveSentAt = System.currentTimeMillis();
  }

  public void onKeepAliveReceived() {
    long sent = keepAliveSentAt;
    if (sent > 0) {
      ping = (int) (System.currentTimeMillis() - sent);
      keepAliveSentAt = -1;
    }
  }

  public void initiateJavaConnection() {
    consoleChannel.config().setAutoRead(false);
    initChunkManager();
    String host;
    int port;

    if (server.getConfig().getNetwork().isNetworkMode()) {
      dev.briiqn.reunion.core.config.ServerEntry def = server.getConfig().getDefaultServer();
      if (def != null) {
        host = def.host();
        port = def.port();
        currentServer = def.name();
      } else {
        host = server.getJavaHost();
        port = server.getJavaPort();
      }
    } else {
      host = server.getJavaHost();
      port = server.getJavaPort();
    }

    final String finalHost = host;
    final int finalPort = port;

    Bootstrap b = new Bootstrap();
    b.group(consoleChannel.eventLoop())
        .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ServerConnector.initJavaPipeline(ch, ConsoleSession.this);
          }
        });

    b.connect(host, port).addListener((ChannelFuture f) -> {
      if (f.isSuccess()) {
        if (!consoleChannel.isActive()) {
          f.channel().close();
          return;
        }
        JavaSession session = f.channel().attr(JavaSession.SESSION_KEY).get();
        this.javaSession = session;
        session.sendHandshake(playerName, finalHost, finalPort);
      } else {
        log.error("[Disconnect] Failed to connect {} to Java server", playerName);
        PacketManager.send(consoleChannel,
            new dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl
                .ConsoleDisconnectS2CPacket(), 39);
        consoleChannel.close();
      }
    });
  }

  public void initChunkManager() {
    if (chunkManager == null) {
      chunkManager = new ChunkManager(this);
    }
  }

  public void onJoinGame(int javaEid, short gamemode, byte dimension, short difficulty,
      String levelType) {
    if (this.javaEntityId != -1 && this.javaEntityId != javaEid) {
      entityManager.remove(this.javaEntityId);
    }
    this.javaEntityId = javaEid;

    if (this.safePlayerId == -1) {
      this.safePlayerId = entityManager.map(javaEid);
    } else {
      entityManager.map(javaEid, this.safePlayerId);
    }
    entityManager.registerCategory(javaEid, EntityManager.EntityCategory.PLAYER);

    this.dimension = dimension;
    this.gamemode = gamemode;
    this.difficulty = difficulty;
    this.levelType = levelType != null ? levelType : "DEFAULT";

    clientScreenReady = false;
    pendingChatQueue.clear();

    PacketManager.sendToConsole(this, new ConsolePreLoginS2CPacket(clientVersion, playerName));
    PacketManager.sendToConsole(this, new ConsoleLoginS2CPacket(
        this.safePlayerId,
        playerName.substring(0, Math.min(playerName.length(), 16)),
        this.levelType, 0L, gamemode, dimension, difficulty,
        smallId, playerPrivileges));
    PacketManager.sendToConsole(this, new ConsoleRespawnS2CPacket(
        dimension, gamemode, this.levelType, 0L, difficulty, this.safePlayerId));

    this.loggedIn = true;
    consoleChannel.config().setAutoRead(true);
  }

  public void cleanupForWorldChange() {
    if (chunkManager != null) {
      chunkManager.unloadAllChunks();
    }

    byte openWindow = inventoryTracker.getWindowId();
    if (openWindow != 0) {
      PacketManager.sendToConsole(this, new ConsoleContainerCloseS2CPacket(openWindow));
    }

    List<Integer> entitiesToRemove = entityManager.clearAndGetConsoleIds();
    entitiesToRemove.remove(Integer.valueOf(safePlayerId));
    if (!entitiesToRemove.isEmpty()) {
      for (int i = 0; i < entitiesToRemove.size(); i += 255) {
        int end = Math.min(i + 255, entitiesToRemove.size());
        PacketManager.sendToConsole(this,
            new ConsoleRemoveEntitiesS2CPacket(entitiesToRemove.subList(i, end)));
      }
    }

    if (chunkManager != null) {
      chunkManager.clearCache();
    }

    inventoryTracker.reset();
    worldOffsetX = 0;
    worldOffsetZ = 0;

    if (javaEntityId != -1) {
      entityManager.map(javaEntityId, safePlayerId);
      entityManager.registerCategory(javaEntityId, EntityManager.EntityCategory.PLAYER);
    }

    clientScreenReady = false;
    pendingChatQueue.clear();
  }

  public void queueTickAction(Runnable action) {
    tickActionQueue.add(action);
  }

  public void flushTickActions() {
    if (!tickActionQueue.isEmpty()) {
      List<Runnable> copy = new ArrayList<>(tickActionQueue);
      tickActionQueue.clear();
      copy.forEach(Runnable::run);
    }
    ticksAlive++;
    if (loggedIn) {
      PluginEventHooks.fireTick(
          new ConsoleSessionPlayerAdapter(this), ticksAlive);
    }
  }

  public void storePendingTeleport(double x, double y, double z) {
    pendingTeleports.add(new Vec3d(x, y, z));
  }

  public boolean hasPendingTeleport() {
    return !pendingTeleports.isEmpty();
  }

  public Vec3d consumePendingTeleport() {
    return pendingTeleports.poll();
  }

  public void setWindowType(int windowId, int type) {
    windowTypes.put(windowId, type);
  }

  public int getWindowType(int windowId) {
    return windowTypes.getOrDefault(windowId, 0);
  }

  public short nextActionId() {
    return actionCounter++;
  }

  public double toConsoleX(double javaX) {
    return worldHack.toConsoleX(javaX);
  }

  public double toConsoleZ(double javaZ) {
    return worldHack.toConsoleZ(javaZ);
  }

  public double toJavaX(double consoleX) {
    return worldHack.toJavaX(consoleX);
  }

  public double toJavaZ(double consoleZ) {
    return worldHack.toJavaZ(consoleZ);
  }

  public int toConsoleX(int javaX) {
    return worldHack.toConsoleX(javaX);
  }

  public int toConsoleZ(int javaZ) {
    return worldHack.toConsoleZ(javaZ);
  }

  public int toJavaX(int consoleX) {
    return worldHack.toJavaX(consoleX);
  }

  public int toJavaZ(int consoleZ) {
    return worldHack.toJavaZ(consoleZ);
  }

  public void checkWorldBounds(double javaX, double javaZ) {
    worldHack.checkWorldBounds(javaX, javaZ);
  }

  public int getHackThreshold() {
    return worldHack.getThreshold();
  }

  public PacketSide getPacketSide(Packet packet) {
    PacketInfo info = PacketRegistry.getPacketInfo(PacketInfo.class);
    return info != null ? info.side() : PacketSide.CONSOLE_S2C;
  }

  public void setCurrentServer(String name) {
    this.previousServerName = this.currentServer;
    this.currentServer = name;
  }

  public void handleEnchantmentTableHack(byte windowId) {
    if (javaSession == null) {
      return;
    }

    Map<Integer, ItemInstance> slots = inventoryTracker.getActiveSlots();

    ItemInstance lapisSlot = slots.get(1);
    if (lapisSlot != null && lapisSlot.id() == 351 && lapisSlot.damage() == 4) {
      return;
    }

    int lapisSource = -1;
    ItemInstance lapisItem = null;
    for (Map.Entry<Integer, ItemInstance> entry : slots.entrySet()) {
      if (entry.getKey() < 2) {
        continue;
      }
      ItemInstance item = entry.getValue();
      if (item != null && item.id() == 351 && item.damage() == 4) {
        lapisSource = entry.getKey();
        lapisItem = item;
        break;
      }
    }

    if (lapisSource != -1) {
      log.debug("[EnchantHack] Moving lapis from slot {} to slot 1 for {}", lapisSource,
          playerName);
      PacketManager.sendToJava(javaSession, new JavaClickWindowC2SPacket(
          windowId, (short) lapisSource, 0, nextActionId(), 0, lapisItem));
      PacketManager.sendToJava(javaSession, new JavaClickWindowC2SPacket(
          windowId, (short) 1, 0, nextActionId(), 0, null));
    }
  }
}