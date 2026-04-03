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

import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityManager {

  private final Int2IntMap javaToConsole = new Int2IntOpenHashMap();
  private final Int2IntMap consoleToJava = new Int2IntOpenHashMap();
  private final Int2IntMap entityTypes = new Int2IntOpenHashMap();
  private final Int2IntMap objectDataValues = new Int2IntOpenHashMap();

  private final Int2ObjectMap<Vec2f> entityRotations = new Int2ObjectOpenHashMap<>();

  private final Int2ObjectMap<EntityCategory> entityCategories = new Int2ObjectOpenHashMap<>();
  private final Map<Integer, Object> entityExtraData = new HashMap<>();

  private final Int2IntMap entityX = new Int2IntOpenHashMap();
  private final Int2IntMap entityY = new Int2IntOpenHashMap();
  private final Int2IntMap entityZ = new Int2IntOpenHashMap();

  private final Map<Integer, String> entitySkinPaths = new HashMap<>();
  private final Map<Integer, String> entityCapePaths = new HashMap<>();

  private int nextConsoleId = 100;

  public int allocConsoleId() {
    int cid = nextConsoleId++;
    if (nextConsoleId > 2000) {
      nextConsoleId = 100;
    }
    return cid;
  }

  public void setRotation(int javaId, byte yaw, byte pitch) {
    entityRotations.put(javaId, new Vec2f(yaw, pitch));
  }

  public Vec2f getRotation(int javaId) {
    Vec2f r = entityRotations.get(javaId);
    return r != null ? r : Vec2f.ZERO;
  }

  public byte getYaw(int javaId) {
    Vec2f r = entityRotations.get(javaId);
    return r != null ? (byte) r.yaw() : 0;
  }

  public byte getPitch(int javaId) {
    Vec2f r = entityRotations.get(javaId);
    return r != null ? (byte) r.pitch() : 0;
  }

  public void setPosition(int javaId, int x, int y, int z) {
    entityX.put(javaId, x);
    entityY.put(javaId, y);
    entityZ.put(javaId, z);
  }

  public void movePosition(int javaId, byte dx, byte dy, byte dz) {
    entityX.put(javaId, entityX.getOrDefault(javaId, 0) + dx);
    entityY.put(javaId, entityY.getOrDefault(javaId, 0) + dy);
    entityZ.put(javaId, entityZ.getOrDefault(javaId, 0) + dz);
  }

  public int getX(int javaId) {
    return entityX.getOrDefault(javaId, 0);
  }

  public int getY(int javaId) {
    return entityY.getOrDefault(javaId, 0);
  }

  public int getZ(int javaId) {
    return entityZ.getOrDefault(javaId, 0);
  }

  public int map(int javaId) {
    if (javaToConsole.containsKey(javaId)) {
      return javaToConsole.get(javaId);
    }
    int cid = allocConsoleId();
    consoleToJava.put(cid, javaId);
    javaToConsole.put(javaId, cid);
    return cid;
  }

  public void map(int javaId, int consoleId) {
    javaToConsole.put(javaId, consoleId);
    consoleToJava.put(consoleId, javaId);
  }

  public Integer getConsoleId(int javaId) {
    return javaToConsole.containsKey(javaId) ? javaToConsole.get(javaId) : null;
  }

  public Integer reverse(int consoleId) {
    return consoleToJava.containsKey(consoleId) ? consoleToJava.get(consoleId) : null;
  }

  public void remove(int javaId) {
    if (javaToConsole.containsKey(javaId)) {
      int cid = javaToConsole.remove(javaId);
      consoleToJava.remove(cid);
    }
    entityTypes.remove(javaId);
    entityRotations.remove(javaId);
    entityX.remove(javaId);
    entityY.remove(javaId);
    entityZ.remove(javaId);
    objectDataValues.remove(javaId);
    entityCategories.remove(javaId);
    entityExtraData.remove(javaId);
    entitySkinPaths.remove(javaId);
    entityCapePaths.remove(javaId);
  }

  public void clear() {
    javaToConsole.clear();
    consoleToJava.clear();
    entityTypes.clear();
    entityRotations.clear();
    entityX.clear();
    entityY.clear();
    entityZ.clear();
    objectDataValues.clear();
    entityCategories.clear();
    entityExtraData.clear();
  }

  public List<Integer> clearAndGetConsoleIds() {
    List<Integer> ids = new ArrayList<>(consoleToJava.keySet());
    clear();
    return ids;
  }

  public void registerType(int javaId, int type) {
    entityTypes.put(javaId, type);
  }

  public Integer getType(int javaId) {
    return entityTypes.containsKey(javaId) ? entityTypes.get(javaId) : null;
  }

  public void registerObjectData(int javaId, int data) {
    objectDataValues.put(javaId, data);
  }

  public int getObjectData(int javaId) {
    return objectDataValues.getOrDefault(javaId, -1);
  }

  public void registerCategory(int javaId, EntityCategory category) {
    entityCategories.put(javaId, category);
  }

  public EntityCategory getCategory(int javaId) {
    return entityCategories.get(javaId);
  }

  public void setExtraData(int javaId, Object data) {
    entityExtraData.put(javaId, data);
  }

  public Object getExtraData(int javaId) {
    return entityExtraData.get(javaId);
  }

  public void setSkinPath(int javaId, String path) {
    entitySkinPaths.put(javaId, path);
  }

  public String getSkinPath(int javaId) {
    return entitySkinPaths.get(javaId);
  }

  public void setCapePath(int javaId, String path) {
    entityCapePaths.put(javaId, path);
  }

  public String getCapePath(int javaId) {
    return entityCapePaths.get(javaId);
  }

  public Collection<Integer> getJavaIds() {
    return new ArrayList<>(javaToConsole.keySet());
  }

  public Collection<Integer> getConsoleIds() {
    return new ArrayList<>(consoleToJava.keySet());
  }

  public enum EntityCategory {
    PLAYER, MOB, OBJECT, PAINTING, EXPERIENCE_ORB, GLOBAL_ENTITY
  }
}