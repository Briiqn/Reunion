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

package dev.briiqn.reunion.api.messaging;

import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.api.plugin.Plugin;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of {@link PluginMessageRegistry}.
 *
 * <p>The actual sending of plugin messages is delegated to {@link PluginMessageSender},
 * a bridge interface that the Reunion core implements to hook into the Netty pipeline.
 */
public final class SimplePluginMessageRegistry implements PluginMessageRegistry {

  private static final Logger log = LogManager.getLogger(SimplePluginMessageRegistry.class);
  private final PluginMessageSender sender;
  private final Map<String, List<Registration>> inbound = new ConcurrentHashMap<>();
  private final Map<String, List<Registration>> outbound = new ConcurrentHashMap<>();
  private final Map<Plugin, List<Registration>> pluginRegs = new ConcurrentHashMap<>();

  public SimplePluginMessageRegistry(PluginMessageSender sender) {
    this.sender = sender;
  }

  @Override
  public void registerInbound(Plugin plugin, String channel, PluginMessageHandler handler) {
    addRegistration(inbound, plugin, channel, handler);
  }

  @Override
  public void registerOutbound(Plugin plugin, String channel, PluginMessageHandler handler) {
    addRegistration(outbound, plugin, channel, handler);
  }

  @Override
  public void register(Plugin plugin, String channel, PluginMessageHandler handler) {
    addRegistration(inbound, plugin, channel, handler);
    addRegistration(outbound, plugin, channel, handler);
  }

  @Override
  public void unregister(Plugin plugin, String channel) {
    removeFromMap(inbound, plugin, channel);
    removeFromMap(outbound, plugin, channel);
  }

  @Override
  public void unregisterAll(Plugin plugin) {
    List<Registration> regs = pluginRegs.remove(plugin);
    if (regs == null) {
      return;
    }
    for (Registration reg : regs) {
      inbound.getOrDefault(reg.channel, Collections.emptyList()).remove(reg);
      outbound.getOrDefault(reg.channel, Collections.emptyList()).remove(reg);
    }
  }

  @Override
  public Set<String> registeredChannels() {
    Set<String> channels = new LinkedHashSet<>();
    channels.addAll(inbound.keySet());
    channels.addAll(outbound.keySet());
    return Collections.unmodifiableSet(channels);
  }

  @Override
  public boolean isRegistered(String channel) {
    return (inbound.containsKey(channel) && !inbound.get(channel).isEmpty())
        || (outbound.containsKey(channel) && !outbound.get(channel).isEmpty());
  }

  @Override
  public void broadcastToServers(String channel, byte[] data) {
    log.warn(
        "[Messaging] broadcastToServers called but no player iterator is set up in this implementation.");
  }

  @Override
  public void sendToServer(Player player, String channel, byte[] data) {
    sender.sendToServer(player, channel, data);
  }

  @Override
  public void sendToClient(Player player, String channel, byte[] data) {
    sender.sendToClient(player, channel, data);
  }

  /**
   * Called by the proxy core when a plugin message arrives from the Java backend. Dispatches to all
   * registered inbound handlers for the channel.
   *
   * @param player  the player whose Java session received the message
   * @param channel the channel
   * @param data    the payload
   */
  public void dispatchInbound(Player player, String channel, byte[] data) {
    dispatch(inbound, player, channel, data);
  }


  /**
   * Called by the proxy core when a plugin message arrives from the console client. Dispatches to
   * all registered outbound handlers for the channel.
   *
   * @param player  the player whose console client sent the message
   * @param channel the channel
   * @param data    the payload
   */
  public void dispatchOutbound(Player player, String channel, byte[] data) {
    dispatch(outbound, player, channel, data);
  }

  private void addRegistration(Map<String, List<Registration>> map, Plugin plugin, String channel,
      PluginMessageHandler handler) {
    Registration reg = new Registration(plugin, channel, handler);
    map.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(reg);
    pluginRegs.computeIfAbsent(plugin, k -> new CopyOnWriteArrayList<>()).add(reg);
  }


  private void removeFromMap(Map<String, List<Registration>> map, Plugin plugin, String channel) {
    List<Registration> regs = map.get(channel);
    if (regs == null) {
      return;
    }
    regs.removeIf(r -> r.plugin == plugin);
  }

  private void dispatch(Map<String, List<Registration>> map, Player player, String channel,
      byte[] data) {
    List<Registration> regs = map.get(channel);
    if (regs == null || regs.isEmpty()) {
      return;
    }
    for (Registration reg : regs) {
      try {
        reg.handler.handle(player, data);
      } catch (Exception e) {
        log.error("[Messaging] Exception in handler for channel '{}' in plugin '{}': {}",
            channel, reg.plugin.getClass().getSimpleName(), e.getMessage(), e);
      }
    }
  }

  /**
   * Bridge to the actual Netty send machinery.
   */
  public interface PluginMessageSender {

    void sendToServer(Player player, String channel, byte[] data);

    void sendToClient(Player player, String channel, byte[] data);
  }

  private record Registration(Plugin plugin, String channel, PluginMessageHandler handler) {

  }
}
