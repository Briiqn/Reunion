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

package dev.briiqn.reunion.core.plugin.hooks;

import dev.briiqn.reunion.api.ReunionProxyImpl;
import dev.briiqn.reunion.api.event.packet.ConsolePacketReceiveEvent;
import dev.briiqn.reunion.api.event.packet.JavaPacketReceiveEvent;
import dev.briiqn.reunion.api.event.packet.JavaPacketSendEvent;
import dev.briiqn.reunion.api.event.packet.PacketSendEvent;
import dev.briiqn.reunion.api.event.packet.PluginMessageEvent;
import dev.briiqn.reunion.api.event.player.LoginEvent;
import dev.briiqn.reunion.api.event.player.PlayerActionEvent;
import dev.briiqn.reunion.api.event.player.PlayerChatEvent;
import dev.briiqn.reunion.api.event.player.PlayerDisconnectEvent;
import dev.briiqn.reunion.api.event.player.PlayerHeldEvent;
import dev.briiqn.reunion.api.event.player.PlayerJoinEvent;
import dev.briiqn.reunion.api.event.player.PlayerMoveEvent;
import dev.briiqn.reunion.api.event.player.PlayerSwingEvent;
import dev.briiqn.reunion.api.event.player.PlayerTickEvent;
import dev.briiqn.reunion.api.event.player.PreJoinEvent;
import dev.briiqn.reunion.api.event.player.PreLoginEvent;
import dev.briiqn.reunion.api.event.player.ServerSwitchEvent;
import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static bridge between the Reunion core packet handlers and the plugin event bus.
 *
 * <p>Call {@link #init(ReunionProxyImpl)} once during server startup. After that, every
 * packet handler calls the relevant {@code fire*()} method here to let plugins observe and
 * intercept the event.
 *
 * <h2>Call sites</h2>
 * <table border="1">
 *   <tr><th>Method</th><th>Where to call it</th></tr>
 *   <tr><td>{@link #firePreLogin}</td>
 *       <td>{@code ConsolePreLoginC2SPacket#handle}  before {@code initiateJavaConnection()}</td></tr>
 *   <tr><td>{@link #isHeld}</td>
 *       <td>{@code ConsoleLoginC2SPacket#handle}  to check if the session was held</td></tr>
 *   <tr><td>{@link #fireHeld}</td>
 *       <td>{@code ConsoleLoginC2SPacket#handle}  when {@code isHeld()} is true</td></tr>
 *   <tr><td>{@link #fireLogin}</td>
 *       <td>{@code ConsoleLoginC2SPacket#handle}  normal (non-held) login path</td></tr>
 *   <tr><td>{@link #firePreJoin}</td>
 *       <td>{@code JavaJoinGameS2CPacket#handle}  after {@code onJoinGame()}, can deny</td></tr>
 *   <tr><td>{@link #fireJoin}</td>
 *       <td>{@code JavaJoinGameS2CPacket#handle}  after {@code firePreJoin} is allowed</td></tr>
 *   <tr><td>{@link #fireDisconnect}</td>
 *       <td>{@code ConsoleSession} channelInactive  before cleanup</td></tr>
 *   <tr><td>{@link #fireChat}</td>
 *       <td>{@code ConsoleChatC2SPacket#handle}  before forwarding to Java</td></tr>
 *   <tr><td>{@link #fireServerSwitch}</td>
 *       <td>{@code ServerSwitchHandler#switchServer}  before closing the old session</td></tr>
 *   <tr><td>{@link #fireConsoleInbound}</td>
 *       <td>Console handler loop  after {@code packet.read()}, before {@code packet.handle()}</td></tr>
 *   <tr><td>{@link #fireJavaInbound}</td>
 *       <td>Java handler loop  after {@code packet.read()}, before {@code packet.handle()}</td></tr>
 *   <tr><td>{@link #fireConsoleSend}</td>
 *       <td>{@code PacketManager#sendToConsole}  before channel write</td></tr>
 *   <tr><td>{@link #fireJavaSend}</td>
 *       <td>{@code PacketManager#sendToJava}  before channel write</td></tr>
 *   <tr><td>{@link #firePluginMessageFromServer}</td>
 *       <td>{@code JavaPluginMessageS2CPacket#handle}</td></tr>
 *   <tr><td>{@link #firePluginMessageFromClient}</td>
 *       <td>{@code ConsoleCustomPayloadC2SPacket#handle}</td></tr>
 * </table>
 */
public final class PluginEventHooks {

  /**
   * Sessions whose {@link PreLoginEvent} result was {@code HELD}. They have a console connection
   * but no Java session yet. Entries are consumed (removed) by {@link #isHeld(ConsoleSession)}.
   */
  private static final Set<ConsoleSession> heldSessions =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  private static ReunionProxyImpl proxy;

  private PluginEventHooks() {
  }

  /**
   * Initialises the hooks. Call this once in {@code ReunionServer.start()} immediately after
   * constructing {@link ReunionProxyImpl}, before loading plugins.
   */
  public static void init(ReunionProxyImpl api) {
    proxy = api;
  }

  private static boolean ready() {
    return proxy != null;
  }

  /**
   * Fires {@link PreLoginEvent} from {@code ConsolePreLoginC2SPacket#handle}.
   *
   * <p>If the result is {@code HELD}, the session is registered internally so
   * {@link #isHeld(ConsoleSession)} returns {@code true} when the Login packet arrives.
   *
   * <pre>{@code
   * InetSocketAddress addr = (InetSocketAddress) session.getConsoleChannel().remoteAddress();
   * PreLoginEvent event = PluginEventHooks.firePreLogin(session, playerName, addr, clientVersion);
   * switch (event.result().status()) {
   *     case DENIED -> { sendDisconnect(session); return; }
   *     case HELD   -> { session.getConsoleChannel().config().setAutoRead(true); return; }
   *     default     -> session.initiateJavaConnection();
   * }
   * }</pre>
   */
  public static PreLoginEvent firePreLogin(ConsoleSession session,
      String username,
      InetSocketAddress address,
      int clientVersion) {
    var event = new PreLoginEvent(username, address, clientVersion);
    if (ready()) {
      proxy.eventBus().fire(event);
    }
    if (event.isHeld()) {
      heldSessions.add(session);
    }
    return event;
  }

  /**
   * Returns {@code true} if this session's {@link PreLoginEvent} was held.
   *
   * <p>This method <b>consumes</b> the flag  calling it twice on the same session
   * always returns {@code false} on the second call. Call it exactly once, from
   * {@code ConsoleLoginC2SPacket#handle}.
   */
  public static boolean isHeld(ConsoleSession session) {
    return heldSessions.remove(session);
  }

  /**
   * Fires {@link PlayerHeldEvent} from {@code ConsoleLoginC2SPacket#handle} when
   * {@link #isHeld(ConsoleSession)} is {@code true}.
   *
   * <p>Any plugin (e.g. a limbo plugin) subscribes to {@link PlayerHeldEvent} to
   * take ownership of the player. No plugin-manager lookup or casting needed.
   *
   * <pre>{@code
   * if (PluginEventHooks.isHeld(session)) {
   *     PluginEventHooks.fireHeld(new ConsoleSessionPlayerAdapter(session));
   *     return;
   * }
   * }</pre>
   */
  public static void fireHeld(Player player) {
    if (ready()) {
      proxy.eventBus().fire(new PlayerHeldEvent(player));
    }
  }

  /**
   * Fires {@link LoginEvent} from {@code ConsoleLoginC2SPacket#handle} on the normal (non-held)
   * path. Returns the event so the caller can inspect the result.
   *
   * <pre>{@code
   * LoginEvent event = PluginEventHooks.fireLogin(new ConsoleSessionPlayerAdapter(session));
   * if (!event.isAllowed()) {
   *     PacketManager.sendToConsole(session, new ConsoleDisconnectS2CPacket());
   *     session.getConsoleChannel().close();
   *     session.getServer().removeSession(username);
   * }
   * }</pre>
   */
  public static LoginEvent fireLogin(Player player) {
    var event = new LoginEvent(player);
    if (ready()) {
      proxy.eventBus().fire(event);
    }
    return event;
  }

  /**
   * Fires {@link PreJoinEvent} from {@code JavaJoinGameS2CPacket#handle}, after
   * {@code cs.onJoinGame()} but before {@link #fireJoin}. Can be denied.
   *
   * <pre>{@code
   * PreJoinEvent pre = PluginEventHooks.firePreJoin(apiPlayer);
   * if (!pre.isAllowed()) {
   *     PacketManager.sendToConsole(cs, new ConsoleDisconnectS2CPacket());
   *     cs.getConsoleChannel().close();
   *     return;
   * }
   * PluginEventHooks.fireJoin(apiPlayer);
   * }</pre>
   */
  public static PreJoinEvent firePreJoin(Player player) {
    var event = new PreJoinEvent(player);
    if (ready()) {
      proxy.eventBus().fire(event);
    }
    return event;
  }

  /**
   * Fires {@link PlayerJoinEvent} from {@code JavaJoinGameS2CPacket#handle} after
   * {@link #firePreJoin} was allowed. The player is now fully online.
   */
  public static void fireJoin(Player player) {
    if (ready()) {
      proxy.eventBus().fire(new PlayerJoinEvent(player));
    }
  }

  /**
   * Fires {@link PlayerDisconnectEvent} from {@code ConsoleSession} channelInactive, before session
   * cleanup. The player object is still accessible during the event.
   *
   * <pre>{@code
   * // In ConsoleSession.consoleChannelHandler() channelInactive:
   * PluginEventHooks.fireDisconnect(new ConsoleSessionPlayerAdapter(ConsoleSession.this), "disconnected");
   * // ... then do cleanup
   * }</pre>
   */
  public static void fireDisconnect(Player player, String reason) {
    if (ready()) {
      proxy.eventBus().fire(new PlayerDisconnectEvent(player, reason));
    }
  }

  /**
   * Fires {@link PlayerChatEvent} from {@code ConsoleChatC2SPacket#handle}. Returns the event so
   * the caller can check {@code isCancelled()} and read the (possibly modified) message.
   *
   * <pre>{@code
   * PlayerChatEvent event = PluginEventHooks.fireChat(apiPlayer, message);
   * if (event.isCancelled()) return;
   * PacketManager.sendToJava(session.getJavaSession(),
   *         new JavaChatMessageC2SPacket(event.message()));
   * }</pre>
   */
  public static PlayerChatEvent fireChat(Player player, String message) {
    var event = new PlayerChatEvent(player, message);
    if (ready()) {
      proxy.eventBus().fire(event);
    }
    return event;
  }

  /**
   * Fires {@link ServerSwitchEvent} from {@code ServerSwitchHandler#switchServer} before the old
   * Java session is closed. Returns the event so the caller can check {@code isCancelled()}.
   *
   * <pre>{@code
   * ServerSwitchEvent event = PluginEventHooks.fireServerSwitch(
   *         apiPlayer, previousServer, target.getName());
   * if (event.isCancelled()) return;
   * }</pre>
   */
  public static ServerSwitchEvent fireServerSwitch(Player player, String from, String to) {
    var event = new ServerSwitchEvent(player, from, to);
    if (ready()) {
      proxy.eventBus().fire(event);
    }
    return event;
  }

  /**
   * Fires {@link ConsolePacketReceiveEvent} in the console handler loop, after
   * {@code packet.read()} and before {@code packet.handle()}.
   *
   * @return {@code true} to continue processing; {@code false} to drop the packet
   *
   * <pre>{@code
   * if (!PluginEventHooks.fireConsoleInbound(apiPlayer, packet, clientVersion)) return;
   * packet.handle(session, clientVersion);
   * }</pre>
   */
  public static boolean fireConsoleInbound(Player player, ConsoleC2SPacket packet, int protocol) {
    if (!ready()) {
      return true;
    }
    var event = new ConsolePacketReceiveEvent(player, packet, protocol);
    proxy.eventBus().fire(event);
    return !event.isCancelled();
  }

  /**
   * Fires {@link JavaPacketReceiveEvent} in the Java handler loop, after {@code packet.read()} and
   * before {@code packet.handle()}.
   *
   * @return {@code true} to continue; {@code false} to drop the packet
   *
   * <pre>{@code
   * if (!PluginEventHooks.fireJavaInbound(apiPlayer, packet, JAVA_PROTOCOL)) return;
   * packet.handle(javaSession, JAVA_PROTOCOL);
   * }</pre>
   */
  public static boolean fireJavaInbound(Player player, JavaS2CPacket packet, int protocol) {
    if (!ready()) {
      return true;
    }
    var event = new JavaPacketReceiveEvent(player, packet, protocol);
    proxy.eventBus().fire(event);
    return !event.isCancelled();
  }

  /**
   * Fires {@link PacketSendEvent} in {@code PacketManager#sendToConsole} before the channel write.
   * Returns {@code false} to suppress the packet.
   *
   * <pre>{@code
   * if (!PluginEventHooks.fireConsoleSend(apiPlayer, packet, clientVersion)) return;
   * channel.writeAndFlush(...);
   * }</pre>
   */
  public static boolean fireConsoleSend(Player player, ConsoleS2CPacket packet, int protocol) {
    if (!ready()) {
      return true;
    }
    var event = new PacketSendEvent(player, packet, protocol);
    proxy.eventBus().fire(event);
    return !event.isCancelled();
  }

  /**
   * Fires {@link JavaPacketSendEvent} in {@code PacketManager#sendToJava} before the channel write.
   * Returns {@code false} to suppress the packet.
   *
   * <pre>{@code
   * if (!PluginEventHooks.fireJavaSend(apiPlayer, packet, JAVA_PROTOCOL)) return;
   * channel.writeAndFlush(...);
   * }</pre>
   */
  public static boolean fireJavaSend(Player player, JavaC2SPacket packet, int protocol) {
    if (!ready()) {
      return true;
    }
    var event = new JavaPacketSendEvent(player, packet, protocol);
    proxy.eventBus().fire(event);
    return !event.isCancelled();
  }

  /**
   * Fires {@link PluginMessageEvent} for a message arriving from the Java backend, then dispatches
   * to registered inbound messaging handlers.
   *
   * @return {@code false} if a plugin cancelled forwarding to the console client
   *
   * <pre>{@code
   * // In JavaPluginMessageS2CPacket#handle:
   * if (!PluginEventHooks.firePluginMessageFromServer(apiPlayer, channel, rawBytes)) return;
   * // forward to console client
   * }</pre>
   */
  public static boolean firePluginMessageFromServer(Player player, String channel, byte[] data) {
    if (!ready()) {
      return true;
    }
    var event = new PluginMessageEvent(player, channel, data,
        PluginMessageEvent.Source.JAVA_SERVER);
    proxy.eventBus().fire(event);
    if (event.isCancelled()) {
      return false;
    }
    proxy.rawMessagingRegistry().dispatchInbound(player, channel, data);
    return true;
  }

  /**
   * Fires {@link PluginMessageEvent} for a message arriving from the console client, then
   * dispatches to registered outbound messaging handlers.
   *
   * @return {@code false} if a plugin cancelled forwarding to the Java backend
   *
   * <pre>{@code
   * // In ConsoleCustomPayloadC2SPacket#handle:
   * if (!PluginEventHooks.firePluginMessageFromClient(apiPlayer, channel, rawBytes)) return;
   * // forward to Java backend
   * }</pre>
   */
  public static boolean firePluginMessageFromClient(Player player, String channel, byte[] data) {
    if (!ready()) {
      return true;
    }
    var event = new PluginMessageEvent(player, channel, data,
        PluginMessageEvent.Source.CONSOLE_CLIENT);
    proxy.eventBus().fire(event);
    if (event.isCancelled()) {
      return false;
    }
    proxy.rawMessagingRegistry().dispatchOutbound(player, channel, data);
    return true;
  }

  /**
   * Fires {@link PlayerMoveEvent} from movement C2S packet handlers
   * ({@code ConsoleMovePlayerPosC2SPacket} and {@code ConsoleMovePlayerPosRotC2SPacket}).
   *
   * @return {@code true} to continue forwarding to the Java backend; {@code false} to cancel
   */
  public static boolean fireMove(Player player,
      dev.briiqn.reunion.api.world.Location from,
      dev.briiqn.reunion.api.world.Location to) {
    if (!ready()) {
      return true;
    }
    var event = new PlayerMoveEvent(player, from, to);
    proxy.eventBus().fire(event);
    return !event.isCancelled();
  }

  /**
   * Fires {@link PlayerSwingEvent} from {@code ConsoleAnimateC2SPacket#handle}.
   *
   * @return {@code true} to continue forwarding; {@code false} to cancel
   */
  public static boolean fireSwing(Player player) {
    if (!ready()) {
      return true;
    }
    var event = new PlayerSwingEvent(player);
    proxy.eventBus().fire(event);
    return !event.isCancelled();
  }

  /**
   * Fires {@link PlayerActionEvent} from {@code ConsolePlayerActionC2SPacket#handle}. The
   * coordinates passed here should already be Java-side (post world-offset).
   *
   * @return {@code true} to continue forwarding; {@code false} to cancel
   */
  public static boolean fireAction(Player player, int action, int javaX, int y, int javaZ,
      int face) {
    if (!ready()) {
      return true;
    }
    var event = new PlayerActionEvent(player, action, javaX, y, javaZ, face);
    proxy.eventBus().fire(event);
    return !event.isCancelled();
  }

  /**
   * Fires {@link PlayerTickEvent} from {@code ConsoleSession#flushTickActions()}. This is a
   * fire-and-forget  the event is not cancellable.
   */
  public static void fireTick(Player player, long ticksAlive) {
    if (!ready()) {
      return;
    }
    proxy.eventBus().fire(new PlayerTickEvent(player, ticksAlive));
  }
}