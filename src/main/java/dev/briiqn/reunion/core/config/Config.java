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

package dev.briiqn.reunion.core.config;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.data.ForwardingMode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Log4j2
@Getter
public class Config {

  private final File file;
  private final Yaml yaml;
  private final ReunionServer server;

  private final Connection connection = new Connection();
  private final Auth auth = new Auth();
  private final Forwarding forwarding = new Forwarding();
  private final Gameplay gameplay = new Gameplay();
  private final Network network = new Network();
  private final Via via = new Via();

  private boolean preventSave = false;

  public Config(ReunionServer server) {
    this.file = new File("config.yml");
    this.server = server;
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    this.yaml = new Yaml(options);
    load();
  }

  @SuppressWarnings("unchecked")
  public void load() {
    if (!file.exists()) {
      log.info("config.yml not found, creating from default template...");
      try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
        if (in == null) {
          log.error("could not find default config.yml in resources!");
          save();
        } else {
          Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
          log.info("default config dropped. please review it and restart.");
          preventSave = true;
        }
      } catch (Exception e) {
        log.error("failed to drop default config: {}", e.getMessage());
      }
      System.exit(0);
      return;
    }

    try (InputStream in = new FileInputStream(file)) {
      Map<String, Object> data = yaml.load(in);
      if (data == null) return;

      Map<String, Object> conn = (Map<String, Object>) data.get("connection");
      if (conn != null) {
        if (conn.containsKey("java-host")) connection.javaHost = (String) conn.get("java-host");
        if (conn.containsKey("java-port")) connection.javaPort = (int) conn.get("java-port");
        if (conn.containsKey("listen-port")) connection.listenPort = (int) conn.get("listen-port");
        if (conn.containsKey("player-prefix")) connection.playerPrefix = (String) conn.get("player-prefix");
      }

      Map<String, Object> authData = (Map<String, Object>) data.get("auth");
      if (authData != null) {
        if (authData.containsKey("online-mode")) auth.onlineMode = (boolean) authData.get("online-mode");
      }

      Map<String, Object> fwd = (Map<String, Object>) data.get("forwarding");
      if (fwd != null) {
        if (fwd.containsKey("mode")) forwarding.mode = ForwardingMode.fromString((String) fwd.get("mode"), ForwardingMode.NONE);
        if (fwd.containsKey("velocity-secret")) forwarding.velocitySecret = (String) fwd.get("velocity-secret");
        if (fwd.containsKey("bungee-guard-token")) forwarding.bungeeGuardToken = (String) fwd.get("bungee-guard-token");
      }

      Map<String, Object> gp = (Map<String, Object>) data.get("gameplay");
      if (gp != null) {
        if (gp.containsKey("auto-accept-resource-packs")) gameplay.autoAcceptResourcePacks = (boolean) gp.get("auto-accept-resource-packs");
        if (gp.containsKey("infinite-world-hack")) gameplay.infiniteWorldHack = (boolean) gp.get("infinite-world-hack");
        if (gp.containsKey("offload-chunks-to-disk")) gameplay.offloadChunksToDisk = (boolean) gp.get("offload-chunks-to-disk");
        if (gp.containsKey("release-item-on-interact")) gameplay.releaseItemOnInteract = (boolean) gp.get("release-item-on-interact");
        if (gp.containsKey("max-players")) gameplay.maxPlayers = (int) gp.get("max-players");
        if (gp.containsKey("min-model-size-blocks")) gameplay.minModelSizeBlocks = ((Number) gp.get("min-model-size-blocks")).doubleValue();
        if (gp.containsKey("max-model-size-blocks")) gameplay.maxModelSizeBlocks = ((Number) gp.get("max-model-size-blocks")).doubleValue();
        if (gp.containsKey("max-model-byte-size")) gameplay.maxModelByteSize = ((Number) gp.get("max-model-byte-size")).intValue();
        if (gp.containsKey("enchantment-table-hack")) gameplay.enchantmentTableHack = (boolean) gp.get("enchantment-table-hack");
      }

      Map<String, Object> net = (Map<String, Object>) data.get("network");
      if (net != null) {
        if (net.containsKey("network-mode")) network.networkMode = (boolean) net.get("network-mode");
        if (net.containsKey("default-server")) network.defaultServer = (String) net.get("default-server");
        if (net.containsKey("netty-threads")) network.nettyThreads = (int) net.get("netty-threads");

        network.servers.clear();
        Map<String, Object> serverMap = (Map<String, Object>) net.get("servers");
        if (serverMap != null) {
          for (Map.Entry<String, Object> entry : serverMap.entrySet()) {
            Map<String, Object> srv = (Map<String, Object>) entry.getValue();
            String host = (String) srv.getOrDefault("host", "127.0.0.1");
            int port = (int) srv.getOrDefault("port", 25564);
            network.servers.put(entry.getKey(), new ServerEntry(entry.getKey(), host, port));
          }
        }
      }

      Map<String, Object> viaData = (Map<String, Object>) data.get("via");
      if (viaData != null) {
        if (viaData.containsKey("enabled")) via.enabled = (boolean) viaData.get("enabled");
        if (viaData.containsKey("target-version")) via.targetVersion = (String) viaData.get("target-version");
      }

    } catch (Exception e) {
      log.error("failed to load config.yml", e);
    }
  }

  public void save() {
    if (preventSave) return;

    Map<String, Object> data = new LinkedHashMap<>();

    Map<String, Object> conn = new LinkedHashMap<>();
    conn.put("java-host", connection.javaHost);
    conn.put("java-port", connection.javaPort);
    conn.put("listen-port", connection.listenPort);
    conn.put("player-prefix", connection.playerPrefix);
    data.put("connection", conn);

    Map<String, Object> authData = new LinkedHashMap<>();
    authData.put("online-mode", auth.onlineMode);
    data.put("auth", authData);

    Map<String, Object> fwd = new LinkedHashMap<>();
    fwd.put("mode", forwarding.mode.name());
    fwd.put("velocity-secret", forwarding.velocitySecret);
    fwd.put("bungee-guard-token", forwarding.bungeeGuardToken);
    data.put("forwarding", fwd);

    Map<String, Object> gp = new LinkedHashMap<>();
    gp.put("auto-accept-resource-packs", gameplay.autoAcceptResourcePacks);
    gp.put("infinite-world-hack", gameplay.infiniteWorldHack);
    gp.put("offload-chunks-to-disk", gameplay.offloadChunksToDisk);
    gp.put("release-item-on-interact", gameplay.releaseItemOnInteract);
    gp.put("max-players", gameplay.maxPlayers);
    gp.put("min-model-size-blocks", gameplay.minModelSizeBlocks);
    gp.put("max-model-size-blocks", gameplay.maxModelSizeBlocks);
    gp.put("max-model-byte-size", gameplay.maxModelByteSize);
    gp.put("enchantment-table-hack", gameplay.enchantmentTableHack);
    data.put("gameplay", gp);

    Map<String, Object> net = new LinkedHashMap<>();
    net.put("network-mode", network.networkMode);
    net.put("default-server", network.defaultServer);
    net.put("netty-threads", network.nettyThreads);

    Map<String, Object> serverMap = new LinkedHashMap<>();
    for (ServerEntry srv : network.servers.values()) {
      Map<String, Object> s = new LinkedHashMap<>();
      s.put("host", srv.host());
      s.put("port", srv.port());
      serverMap.put(srv.name(), s);
    }
    net.put("servers", serverMap);
    data.put("network", net);

    Map<String, Object> viaData = new LinkedHashMap<>();
    viaData.put("enabled", via.enabled);
    viaData.put("target-version", via.targetVersion);
    data.put("via", viaData);

    try (FileWriter writer = new FileWriter(file)) {
      yaml.dump(data, writer);
    } catch (Exception e) {
      log.error("failed to save config.yml", e);
    }
  }

  public ServerEntry getServer(String name) {
    return network.servers.get(name);
  }

  public ServerEntry getDefaultServer() {
    return network.servers.get(network.defaultServer);
  }

  @Getter
  public static class Connection {
    private String javaHost = "127.0.0.1";
    private int javaPort = 25564;
    private int listenPort = 25565;
    private String playerPrefix = "_";
  }

  @Getter
  public static class Auth {
    private boolean onlineMode = false;
  }

  @Getter
  public static class Forwarding {
    private ForwardingMode mode = ForwardingMode.NONE;
    private String velocitySecret = "";
    private String bungeeGuardToken = "";

    public boolean isEnabled() { return mode != ForwardingMode.NONE; }
    public boolean isVelocity() { return mode == ForwardingMode.VELOCITY; }
    public boolean isBungeeCord() { return mode == ForwardingMode.BUNGEECORD || mode == ForwardingMode.BUNGEEGUARD; }
    public boolean isBungeeGuard() { return mode == ForwardingMode.BUNGEEGUARD; }
  }

  @Getter
  public static class Gameplay {
    private boolean autoAcceptResourcePacks = true;
    private boolean infiniteWorldHack = false;
    private boolean offloadChunksToDisk = false;
    private boolean releaseItemOnInteract = true;
    private int maxPlayers = 20;
    private double minModelSizeBlocks = 0.5;
    private double maxModelSizeBlocks = 2.0;
    private int maxModelByteSize = 131072;
    private boolean enchantmentTableHack = true;
  }

  @Getter
  public static class Network {
    private final Map<String, ServerEntry> servers = new LinkedHashMap<>();
    private boolean networkMode = false;
    private String defaultServer = "lobby";
    private int nettyThreads = 4;
  }

  @Getter
  public static class Via {
    private boolean enabled = false;
    private String targetVersion = "auto";
  }
}