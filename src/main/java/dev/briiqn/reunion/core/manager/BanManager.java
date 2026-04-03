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

package dev.briiqn.reunion.core.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Log4j2
public class BanManager {

  private final File banFile;
  private final File ipBanFile;
  private final Yaml yaml;
  private Map<String, String> bans = new HashMap<>();
  private Map<String, String> ipBans = new HashMap<>();

  public BanManager() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    this.yaml = new Yaml(options);
    this.banFile = new File("bans.yml");
    this.ipBanFile = new File("ip-bans.yml");
    load();
  }

  @SuppressWarnings("unchecked")
  public void load() {
    bans = loadFile(banFile);
    ipBans = loadFile(ipBanFile);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> loadFile(File file) {
    if (!file.exists()) {
      saveFile(file, new HashMap<>());
      return new HashMap<>();
    }
    try (InputStream in = new FileInputStream(file)) {
      Map<String, String> data = yaml.load(in);
      return data != null ? new HashMap<>(data) : new HashMap<>();
    } catch (Exception e) {
      log.error("Failed to load {}", file.getName(), e);
      return new HashMap<>();
    }
  }

  private void saveFile(File file, Map<String, String> data) {
    try (FileWriter writer = new FileWriter(file)) {
      yaml.dump(data, writer);
    } catch (Exception e) {
      log.error("Failed to save {}", file.getName(), e);
    }
  }

  public boolean isBanned(String username) {
    return bans.containsKey(username.toLowerCase());
  }

  public String getBanReason(String username) {
    return bans.getOrDefault(username.toLowerCase(), "Banned by an operator.");
  }

  public void ban(String username, String reason) {
    bans.put(username.toLowerCase(), reason);
    saveFile(banFile, bans);
  }

  public void unban(String username) {
    bans.remove(username.toLowerCase());
    saveFile(banFile, bans);
  }

  public boolean isIpBanned(String ip) {
    return ipBans.containsKey(ip);
  }

  public String getIpBanReason(String ip) {
    return ipBans.getOrDefault(ip, "Banned by an operator.");
  }

  public void banIp(String ip, String reason) {
    ipBans.put(ip, reason);
    saveFile(ipBanFile, ipBans);
  }

  public void unbanIp(String ip) {
    ipBans.remove(ip);
    saveFile(ipBanFile, ipBans);
  }
}