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

package dev.briiqn.reunion.core.plugin.session;

import dev.briiqn.reunion.api.packet.PacketContext;
import dev.briiqn.reunion.api.packet.PacketDirection;
import dev.briiqn.reunion.api.packet.PacketInterceptor;
import dev.briiqn.reunion.api.packet.PacketPipeline;
import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.api.plugin.Plugin;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements {@link PacketPipeline} for a single player session.
 */
public final class ConsoleSessionPacketPipeline implements PacketPipeline {

  private static final Logger log = LogManager.getLogger(ConsoleSessionPacketPipeline.class);

  private final ConsoleSession session;

  private final Map<PacketDirection, List<InterceptorRegistration>> interceptors =
      new ConcurrentHashMap<>();

  public ConsoleSessionPacketPipeline(ConsoleSession session) {
    this.session = session;
    for (PacketDirection dir : PacketDirection.values()) {
      interceptors.put(dir, new CopyOnWriteArrayList<>());
    }
  }


  @Override
  public void addInterceptor(Plugin plugin, PacketDirection direction,
      PacketInterceptor interceptor) {
    interceptors.get(direction).add(new InterceptorRegistration(plugin, interceptor));
  }

  @Override
  public void removeInterceptor(PacketInterceptor interceptor) {
    for (List<InterceptorRegistration> list : interceptors.values()) {
      list.removeIf(r -> r.interceptor() == interceptor);
    }
  }

  @Override
  public void removeAll(Plugin plugin) {
    for (List<InterceptorRegistration> list : interceptors.values()) {
      list.removeIf(r -> r.plugin() == plugin);
    }
  }

  @Override
  public void sendToConsoleClient(ConsoleS2CPacket packet) {
    PacketManager.sendToConsole(session, packet);
  }

  @Override
  public void sendToJavaServer(JavaC2SPacket packet) {

    PacketManager.sendToJava(session.getJavaSession(), packet);
  }

  @Override
  public void injectFromConsole(ConsoleC2SPacket packet) {
    if (processConsoleInbound(packet)) {
      packet.handle(session, session.getClientVersion());
    }
  }

  @Override
  public void injectFromJava(JavaS2CPacket packet) {

    if (processJavaInbound(packet)) {
      packet.handle(session.getJavaSession(), JavaSession.JAVA_PROTOCOL);
    }
  }

  public boolean processConsoleInbound(ConsoleC2SPacket packet) {
    return runInterceptors(PacketDirection.CONSOLE_TO_PROXY, packet, session.getClientVersion());
  }

  public boolean processJavaInbound(JavaS2CPacket packet) {
    return runInterceptors(PacketDirection.JAVA_TO_PROXY, packet, JavaSession.JAVA_PROTOCOL);
  }

  public boolean processConsoleOutbound(ConsoleS2CPacket packet) {
    return runInterceptors(PacketDirection.PROXY_TO_CONSOLE, packet, session.getClientVersion());
  }

  public boolean processJavaOutbound(JavaC2SPacket packet) {
    return runInterceptors(PacketDirection.PROXY_TO_JAVA, packet, JavaSession.JAVA_PROTOCOL);
  }

  private boolean runInterceptors(PacketDirection direction, Packet packet, int protocol) {
    List<InterceptorRegistration> list = interceptors.get(direction);
    if (list.isEmpty()) {
      return true;
    }
    Player player = new ConsoleSessionPlayerAdapter(session);
    PacketContextImpl ctx = new PacketContextImpl(player, direction, packet, protocol);

    for (InterceptorRegistration reg : list) {
      try {
        if (!reg.interceptor().intercept(ctx)) {
          return false;
        }
      } catch (Exception e) {
        log.error("[PacketPipeline] Exception in interceptor from plugin {}: {}",
            reg.plugin().getClass().getSimpleName(), e.getMessage(), e);
      }
    }
    return true;
  }

  private record InterceptorRegistration(Plugin plugin, PacketInterceptor interceptor) {

  }

  private record PacketContextImpl(
      Player player,
      PacketDirection direction,
      Packet packet,
      int protocolVersion
  ) implements PacketContext {

  }
}