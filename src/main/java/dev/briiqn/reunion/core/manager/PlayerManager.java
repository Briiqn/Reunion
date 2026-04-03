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

import dev.briiqn.reunion.core.data.PlayerData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.UUID;

public final class PlayerManager {

  private final Object2ObjectOpenHashMap<UUID, PlayerData> dataMap = new Object2ObjectOpenHashMap<>();
  private int nextRemoteSmallId = 10;
  private int nextColorIdx = 1;

  private PlayerData getOrCompute(UUID uuid) {
    return dataMap.computeIfAbsent(uuid, k -> new PlayerData());
  }

  public void setSkin(UUID uuid, String skinName) {
    getOrCompute(uuid).setSkin(skinName);
  }

  public String getSkin(UUID uuid) {
    PlayerData pd = dataMap.get(uuid);
    return pd != null ? pd.getSkin() : null;
  }

  public void setCape(UUID uuid, String capeName) {
    getOrCompute(uuid).setCape(capeName);
  }

  public String getCape(UUID uuid) {
    PlayerData pd = dataMap.get(uuid);
    return pd != null ? pd.getCape() : null;
  }

  public void setConsoleId(UUID uuid, int consoleId) {
    getOrCompute(uuid).setConsoleId(consoleId);
  }

  public Integer getConsoleId(UUID uuid) {
    PlayerData pd = dataMap.get(uuid);
    return pd != null ? pd.getConsoleId() : null;
  }

  public byte addPlayer(UUID uuid, String name) {
    PlayerData pd = getOrCompute(uuid);
    pd.setName(name);

    byte sid = (byte) nextRemoteSmallId++;
    if ((nextRemoteSmallId & 0xFF) > 200) {
      nextRemoteSmallId = 10;
    }
    pd.setSmallId(sid);

    int cidx = nextColorIdx++;
    if (nextColorIdx > 7) {
      nextColorIdx = 1;
    }
    pd.setColorIdx(cidx);

    return sid;
  }

  public Byte removePlayer(UUID uuid) {
    PlayerData pd = dataMap.remove(uuid);
    return pd != null ? pd.getSmallId() : null;
  }

  public void clearConsoleIds() {
    for (PlayerData pd : dataMap.values()) {
      pd.setConsoleId(null);
    }
  }

  public void clear() {
    dataMap.clear();
    nextRemoteSmallId = 10;
    nextColorIdx = 1;
  }

  public String getName(UUID uuid) {
    PlayerData pd = dataMap.get(uuid);
    return pd != null && pd.getName() != null ? pd.getName() : "Player";
  }

  public Byte getSmallId(UUID uuid) {
    PlayerData pd = dataMap.get(uuid);
    return pd != null ? pd.getSmallId() : null;
  }

  public Integer getColorIdx(UUID uuid) {
    PlayerData pd = dataMap.get(uuid);
    return pd != null ? pd.getColorIdx() : null;
  }


}