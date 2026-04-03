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

package dev.briiqn.reunion.api;

import dev.briiqn.reunion.api.command.CommandManager;
import dev.briiqn.reunion.api.command.SimpleCommandManager;
import dev.briiqn.reunion.api.event.EventBus;
import dev.briiqn.reunion.api.event.SimpleEventBus;
import dev.briiqn.reunion.api.messaging.PluginMessageRegistry;
import dev.briiqn.reunion.api.messaging.SimplePluginMessageRegistry;
import dev.briiqn.reunion.api.permission.PermissionManager;
import dev.briiqn.reunion.api.player.Player;
import dev.briiqn.reunion.api.player.PlayerManager;
import dev.briiqn.reunion.api.plugin.PluginLoader;
import dev.briiqn.reunion.api.plugin.PluginManager;
import dev.briiqn.reunion.api.plugin.PluginManagerImpl;
import dev.briiqn.reunion.api.scheduler.GlobalScheduler;
import dev.briiqn.reunion.api.scheduler.SimpleGlobalScheduler;
import dev.briiqn.reunion.api.world.WorldView;
import dev.briiqn.reunion.core.permission.PermissionManagerImpl;
import dev.briiqn.reunion.core.plugin.hooks.PermissionHooks;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The central implementation of {@link ReunionProxy}.
 *
 * <p>One instance is created by the Reunion server on startup and held as a singleton.
 * The constructor accepts a {@link ServerBridge} so this class stays decoupled from the internal
 * {@code ReunionServer} / {@code ConsoleSession} internals.
 *
 * <h2>Integration into the existing server</h2>
 * <p>In {@code ReunionServer}, add:
 * <pre>{@code
 * private ReunionProxyImpl pluginApi;
 *
 * pluginApi = new ReunionProxyImpl(bridge, Path.of("plugins"));
 * pluginApi.start();
 * }</pre>
 *
 * <p>In the login handler, fire events and let the api cancel connections:
 * <pre>{@code
 * if (!pluginApi.eventBus().fireAndCheck(new LoginEvent(player))) {
 *     session.disconnect("Denied by plugin");
 * }
 * }</pre>
 */
public final class ReunionProxyImpl implements ReunionProxy {

  private final EventBus eventBus = new SimpleEventBus();
  private final SimpleCommandManager commandManager = new SimpleCommandManager();
  private final GlobalScheduler globalScheduler = new SimpleGlobalScheduler();

  private final SimplePluginMessageRegistry messagingRegistry;
  private final PluginLoader pluginLoader;
  private final PluginManager pluginManager;
  private final ServerBridge bridge;
  private final PermissionManagerImpl permissionManager;

  public ReunionProxyImpl(ServerBridge bridge, Path pluginsDir) {
    this.bridge = bridge;

    this.permissionManager = new PermissionManagerImpl(Path.of("data"));
    PermissionHooks.init(this.permissionManager);

    this.messagingRegistry = new SimplePluginMessageRegistry(
        new SimplePluginMessageRegistry.PluginMessageSender() {
          @Override
          public void sendToServer(Player player, String channel, byte[] data) {
            bridge.sendToJava(player, channel, data);
          }

          @Override
          public void sendToClient(Player player, String channel, byte[] data) {
            bridge.sendToConsole(player, channel, data);
          }
        }
    );
    this.pluginLoader = new PluginLoader(this, globalScheduler, pluginsDir);
    this.pluginManager = new PluginManagerImpl(pluginLoader);
  }

  public void start() {
    pluginLoader.loadAll();
  }

  public void stop() {
    pluginManager.plugins().forEach(p ->
        pluginManager.plugin(
            p.getClass().getAnnotation(dev.briiqn.reunion.api.annotation.ReunionPlugin.class).id()
        ).ifPresent(__ -> {
        })
    );
    globalScheduler.shutdown();
    permissionManager.close();
  }

  @Override
  public Collection<? extends Player> allPlayers() {
    return bridge.allPlayers();
  }

  @Override
  public Optional<Player> player(String username) {
    return bridge.player(username);
  }

  @Override
  public Optional<Player> player(UUID uuid) {
    return bridge.player(uuid);
  }

  @Override
  public int playerCount() {
    return bridge.allPlayers().size();
  }

  @Override
  public int maxPlayers() {
    return bridge.maxPlayers();
  }

  @Override
  public PluginManager pluginManager() {
    return pluginManager;
  }

  @Override
  public EventBus eventBus() {
    return eventBus;
  }

  @Override
  public CommandManager commandManager() {
    return commandManager;
  }

  @Override
  public GlobalScheduler scheduler() {
    return globalScheduler;
  }

  @Override
  public PlayerManager playerManager() {
    return new PlayerManager() {
      @Override
      public Collection<? extends Player> onlinePlayers() {
        return bridge.allPlayers();
      }

      @Override
      public Optional<Player> player(String username) {
        return bridge.player(username);
      }

      @Override
      public Optional<Player> player(UUID uuid) {
        return bridge.player(uuid);
      }

      @Override
      public int count() {
        return bridge.allPlayers().size();
      }
    };
  }

  @Override
  public PluginMessageRegistry messagingRegistry() {
    return messagingRegistry;
  }

  @Override
  public PermissionManager permissionManager() {
    return permissionManager;
  }

  @Override
  public WorldView worldView(Player player) {
    return bridge.worldView(player);
  }

  @Override
  public String version() {
    return bridge.version();
  }

  @Override
  public boolean isRunning() {
    return bridge.isRunning();
  }

  @Override
  public void shutdown(String reason) {
    bridge.shutdown(reason);
  }

  public SimplePluginMessageRegistry rawMessagingRegistry() {
    return messagingRegistry;
  }

  public SimpleCommandManager rawCommandManager() {
    return commandManager;
  }

  public ServerBridge rawServerBridge() {
    return bridge;
  }

  public interface ServerBridge {

    Collection<? extends Player> allPlayers();

    Optional<Player> player(String username);

    Optional<Player> player(UUID uuid);

    int maxPlayers();

    void disconnectPlayer(Player player, String reason);

    void disconnectBackend(Player player);

    void connectPlayer(Player player, String serverName);

    void teleportPlayer(Player player, dev.briiqn.reunion.api.world.Location location);

    void sendToConsole(Player player, String channel, byte[] data);

    void sendToJava(Player player, String channel, byte[] data);

    WorldView worldView(Player player);

    String version();

    boolean isRunning();

    void shutdown(String reason);
  }
}