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

package dev.briiqn.reunion.api.packet;

import dev.briiqn.reunion.api.plugin.Plugin;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.ConsoleS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.JavaC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;

/**
 * Low-level API for intercepting, cancelling, and injecting typed packets into a specific player's
 * pipeline.
 *
 * <p>Obtain an instance via {@link dev.briiqn.reunion.api.player.Player#packetPipeline()}.
 *
 * <p>This API works with the same strongly-typed packet classes the Reunion core uses.
 * Prefer the higher-level event API
 * ({@link dev.briiqn.reunion.api.event.packet.ConsolePacketReceiveEvent}, etc.) for most use-cases.
 * Use this when you need to inject synthetic packets or when you need per-player interception with
 * state.
 *
 * <h2>Interceptors</h2>
 * <p>Interceptors run before each packet's {@code handle()} method. Return {@code false}
 * from {@link PacketInterceptor#intercept} to drop a packet entirely.
 *
 * <h2>Sending packets</h2>
 * <pre>{@code
 *  * player.packetPipeline().sendToConsoleClient(new ConsoleChatS2CPacket("§aHello!"));
 *
 *  * player.packetPipeline().sendToJavaServer(new JavaChatMessageC2SPacket("/say hi"));
 * }</pre>
 *
 * <h2>Injection</h2>
 * <p>Inject a synthetic packet into the pipeline as if it had just arrived  it will
 * pass through all registered interceptors and then be handled normally:
 * <pre>{@code
 *  * player.packetPipeline().injectFromConsole(new ConsoleChatC2SPacket("hello world"));
 * }</pre>
 */
public interface PacketPipeline {

  /**
   * Registers a packet interceptor for packets travelling in the specified direction.
   *
   * <p>The interceptor receives a {@link PacketContext} containing the fully
   * decoded, typed packet. Return {@code false} to drop the packet.
   *
   * @param plugin      the owning plugin (auto-removed when the plugin is disabled)
   * @param direction   which direction to intercept
   * @param interceptor the interceptor callback
   */
  void addInterceptor(Plugin plugin, PacketDirection direction, PacketInterceptor interceptor);

  /**
   * Removes a specific interceptor.
   *
   * @param interceptor the interceptor to remove
   */
  void removeInterceptor(PacketInterceptor interceptor);

  /**
   * Removes all interceptors registered by the given plugin on this pipeline.
   *
   * @param plugin the owning plugin
   */
  void removeAll(Plugin plugin);

  /**
   * Encodes and sends a {@link ConsoleS2CPacket} directly to the console client, bypassing
   * Reunion's normal translation logic.
   *
   * <p>The {@link dev.briiqn.reunion.api.event.packet.PacketSendEvent} is still fired.
   *
   * @param packet the packet to send
   */
  void sendToConsoleClient(ConsoleS2CPacket packet);

  /**
   * Encodes and sends a {@link JavaC2SPacket} directly to the Java backend server, bypassing normal
   * handle() logic.
   *
   * <p>The {@link dev.briiqn.reunion.api.event.packet.JavaPacketSendEvent} is still fired.
   *
   * @param packet the packet to send
   */
  void sendToJavaServer(JavaC2SPacket packet);

  /**
   * Injects a synthetic {@link ConsoleC2SPacket} into the inbound pipeline exactly as if it had
   * just been decoded from the console client's channel.
   *
   * <p>All registered interceptors for {@link PacketDirection#CONSOLE_TO_PROXY}
   * will run on it, and if none drop it, {@code handle(ConsoleSession)} will be called.
   *
   * @param packet the packet to inject
   */
  void injectFromConsole(ConsoleC2SPacket packet);

  /**
   * Injects a synthetic {@link JavaS2CPacket} into the inbound pipeline exactly as if it had just
   * been decoded from the Java backend's channel.
   *
   * <p>All registered interceptors for {@link PacketDirection#JAVA_TO_PROXY}
   * will run, and if none drop it, {@code handle(JavaSession)} will be called.
   *
   * @param packet the packet to inject
   */
  void injectFromJava(JavaS2CPacket packet);
}

