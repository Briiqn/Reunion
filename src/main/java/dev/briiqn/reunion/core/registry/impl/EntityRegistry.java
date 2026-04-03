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

package dev.briiqn.reunion.core.registry.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.data.Entity;
import dev.briiqn.reunion.core.registry.Registry;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class EntityRegistry extends Registry<Entity> {

  private static final Set<String> UNSUPPORTED_MOB_NAMES = Set.of(
      "Endermite", "Guardian", "Rabbit", "ArmorStand"
  );
  private static final Set<String> UNSUPPORTED_OBJECT_NAMES = Set.of(
      "ArmorStand"
  );
  private static volatile EntityRegistry INSTANCE;
  private final Int2ObjectMap<Entity> mobsById;
  private final Int2ObjectMap<Entity> objectsById;
  private final Int2ObjectMap<List<Entity>> objectsByIdAll;
  private final Map<String, Entity> byName;
  private final Int2IntMap mobRemapTable;

  private EntityRegistry(
      Int2ObjectMap<Entity> mobsById,
      Int2ObjectMap<Entity> objectsById,
      Int2ObjectMap<List<Entity>> objectsByIdAll,
      Map<String, Entity> byName,
      Int2IntMap mobRemapTable
  ) {
    this.mobsById = Int2ObjectMaps.unmodifiable(mobsById);
    this.objectsById = Int2ObjectMaps.unmodifiable(objectsById);
    this.objectsByIdAll = Int2ObjectMaps.unmodifiable(objectsByIdAll);
    this.byName = Collections.unmodifiableMap(byName);
    this.mobRemapTable = Int2IntMaps.unmodifiable(mobRemapTable);
  }

  public static EntityRegistry getInstance() {
    if (INSTANCE == null) {
      synchronized (EntityRegistry.class) {
        if (INSTANCE == null) {
          INSTANCE = load();
        }
      }
    }
    return INSTANCE;
  }

  private static int computeMobFallback(int typeId, Int2ObjectMap<Entity> mobsById) {
    Entity original = mobsById.get(typeId);
    if (original == null || original.hasBoundingBox()) {
      return 54;
    }

    int bestId = 54;
    double bestDist = Double.MAX_VALUE;

    for (Entity candidate : mobsById.values()) {
      if (UNSUPPORTED_MOB_NAMES.contains(candidate.name()) || candidate.hasBoundingBox()) {
        continue;
      }
      if (candidate.width() > original.width() || candidate.height() > original.height()) {
        continue;
      }

      double dw = original.width() - candidate.width();
      double dh = original.height() - candidate.height();
      double dist = Math.sqrt(dw * dw + dh * dh);

      if (dist < bestDist) {
        bestDist = dist;
        bestId = candidate.id();
      }
    }
    return bestId;
  }

  private static EntityRegistry load() {
    try {
      JSONArray json = readJsonArray(EntityRegistry.class, "/entities.json");

      Int2ObjectOpenHashMap<Entity> mobs = new Int2ObjectOpenHashMap<>();
      Int2ObjectOpenHashMap<Entity> objects = new Int2ObjectOpenHashMap<>();
      Int2ObjectOpenHashMap<List<Entity>> objectsAll = new Int2ObjectOpenHashMap<>();
      Map<String, Entity> byName = new LinkedHashMap<>();

      for (int i = 0; i < json.size(); i++) {
        JSONObject obj = json.getJSONObject(i);

        int id = obj.getIntValue("id");
        int internalId = obj.containsKey("internalId") ? obj.getIntValue("internalId") : id;
        String name = obj.getString("name");
        String displayName = obj.getString("displayName");
        String typeStr = obj.getString("type");
        String category = obj.getString("category");
        float width = obj.get("width") != null ? obj.getFloatValue("width") : -1f;
        float height = obj.get("height") != null ? obj.getFloatValue("height") : -1f;

        Entity.Type type = "mob".equals(typeStr) ? Entity.Type.MOB : Entity.Type.OBJECT;
        Entity entity = new Entity(id, internalId, name, displayName,
            type, width, height, category != null ? category : "");

        if (type == Entity.Type.MOB) {
          mobs.putIfAbsent(id, entity);
        } else {
          objects.putIfAbsent(id, entity);
          objectsAll.computeIfAbsent(id, k -> new ArrayList<>()).add(entity);
        }

        byName.putIfAbsent(name, entity);
      }

      Int2IntOpenHashMap mobRemapTable = new Int2IntOpenHashMap(mobs.size());
      for (Int2ObjectMap.Entry<Entity> entry : mobs.int2ObjectEntrySet()) {
        int id = entry.getIntKey();
        mobRemapTable.put(id, UNSUPPORTED_MOB_NAMES.contains(entry.getValue().name())
            ? computeMobFallback(id, mobs)
            : id);
      }

      log.info("[EntityRegistry] Loaded " + mobs.size() + " mobs, "
          + objectsAll.values().stream().mapToInt(List::size).sum()
          + " objects, created " + mobRemapTable.size() + " remap entries");
      return new EntityRegistry(mobs, objects, objectsAll, byName, mobRemapTable);

    } catch (Exception e) {
      log.error("[EntityRegistry] Failed to load entity data", e);
      return new EntityRegistry(
          new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>(),
          new Int2ObjectOpenHashMap<>(), new LinkedHashMap<>(), new Int2IntOpenHashMap());
    }
  }

  @Override
  public Entity get(int id) {
    return get(Entity.Type.MOB, id);
  }

  public Entity get(Entity.Type type, int id) {
    return type == Entity.Type.MOB ? mobsById.get(id) : objectsById.get(id);
  }

  public List<Entity> getAll(Entity.Type type, int id) {
    if (type == Entity.Type.MOB) {
      Entity e = mobsById.get(id);
      return e != null ? List.of(e) : Collections.emptyList();
    }
    return objectsByIdAll.getOrDefault(id, Collections.emptyList());
  }

  public Entity getByName(String name) {
    return byName.get(name);
  }

  @Override
  public boolean isUnsupported(int id) {
    return isUnsupported(Entity.Type.MOB, id);
  }

  public boolean isUnsupported(Entity.Type type, int id) {
    Entity e = get(type, id);
    if (e == null) {
      return false;
    }
    return type == Entity.Type.MOB
        ? UNSUPPORTED_MOB_NAMES.contains(e.name())
        : UNSUPPORTED_OBJECT_NAMES.contains(e.name());
  }

  @Override
  public int remap(int id) {
    return mobRemapTable.getOrDefault(id, id);
  }
}