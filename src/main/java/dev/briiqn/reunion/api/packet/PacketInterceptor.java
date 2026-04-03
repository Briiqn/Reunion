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

/**
 * A functional interface for intercepting packets in the pipeline.
 *
 * <p>Register instances via {@link PacketPipeline#addInterceptor}.
 *
 * <p>The {@link PacketContext} passed to {@link #intercept} carries the fully
 * decoded, typed packet object. Use Java 21 pattern matching to inspect specific packet types
 * without casting:
 *
 * <pre>{@code
 *  * pipeline.addInterceptor(plugin, PacketDirection.CONSOLE_TO_PROXY, ctx -> {
 *     if (frozenPlayers.contains(ctx.player().uuid())) {
 *         if (ctx.packet() instanceof ConsoleMovePlayerPosC2SPacket
 *                 || ctx.packet() instanceof ConsoleMovePlayerPosRotC2SPacket
 *                 || ctx.packet() instanceof ConsoleMovePlayerC2SPacket) {
 *             return false;  *         }
 *     }
 *     return true;
 * });
 * }</pre>
 *
 * <pre>{@code
 *  * pipeline.addInterceptor(plugin, PacketDirection.CONSOLE_TO_PROXY, ctx -> {
 *     if (ctx.packet() instanceof ConsoleChatC2SPacket chat) {
 *          *          *         chat.setMessage(filter(chat.getMessage()));
 *     }
 *     return true;
 * });
 * }</pre>
 */
@FunctionalInterface
public interface PacketInterceptor {

  /**
   * Called when a packet is passing through the pipeline in the registered direction.
   *
   * @param ctx the packet context  direction, typed packet, player, protocol version
   * @return {@code true} to allow the packet to continue (and have {@code handle()} called),
   * {@code false} to silently drop it
   */
  boolean intercept(PacketContext ctx);
}

