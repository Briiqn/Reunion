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

import com.alibaba.fastjson2.JSONObject;
import dev.briiqn.reunion.core.registry.Registry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class LanguageRegistry extends Registry<String> {

  public static final String DEFAULT_LANG = "en_us";

  private static volatile LanguageRegistry INSTANCE;

  private final Map<String, Map<String, String>> languages = new ConcurrentHashMap<>();

  private LanguageRegistry(Map<String, String> defaultEntries) {
    languages.put(DEFAULT_LANG, Collections.unmodifiableMap(defaultEntries));
  }

  public static LanguageRegistry getInstance() {
    if (INSTANCE == null) {
      synchronized (LanguageRegistry.class) {
        if (INSTANCE == null) {
          INSTANCE = load();
        }
      }
    }
    return INSTANCE;
  }

  private static LanguageRegistry load() {
    try {
      JSONObject json = readJsonObject(LanguageRegistry.class, "/language.json");
      Map<String, String> table = new LinkedHashMap<>(json.size());
      for (String key : json.keySet()) {
        table.put(key, json.getString(key));
      }
      log.info("[LanguageRegistry] Loaded " + table.size() + " entries for '" + DEFAULT_LANG + "'");
      return new LanguageRegistry(table);
    } catch (Exception e) {
      log.error("[LanguageRegistry] Failed to load default language data", e);
      return new LanguageRegistry(new LinkedHashMap<>());
    }
  }

  public String translate(String key) {
    return translate(key, DEFAULT_LANG);
  }

  public String translate(String key, String langCode) {
    Map<String, String> table = getOrLoadTable(langCode);
    if (table != null) {
      String value = table.get(key);
      if (value != null) {
        return value;
      }
    }
    if (!DEFAULT_LANG.equals(langCode)) {
      Map<String, String> defaultTable = languages.get(DEFAULT_LANG);
      if (defaultTable != null) {
        String value = defaultTable.get(key);
        if (value != null) {
          return value;
        }
      }
    }
    return key;
  }

  public String translateItemKey(String name, String displayName) {
    String blockKey = "tile." + name + ".name";
    String translated = translate(blockKey);
    if (!translated.equals(blockKey)) {
      return translated;
    }

    String itemKey = "item." + name + ".name";
    translated = translate(itemKey);
    if (!translated.equals(itemKey)) {
      return translated;
    }

    return displayName;
  }

  public boolean isLoaded(String langCode) {
    return languages.containsKey(langCode);
  }

  public Set<String> keys() {
    Map<String, String> table = languages.get(DEFAULT_LANG);
    return table != null ? table.keySet() : Collections.emptySet();
  }

  @Override
  @Deprecated
  public String get(int id) {
    return null;
  }

  @Override
  @Deprecated
  public boolean isUnsupported(int id) {
    return true;
  }

  @Override
  @Deprecated
  public int remap(int id) {
    return -1;
  }

  private Map<String, String> getOrLoadTable(String langCode) {
    Map<String, String> existing = languages.get(langCode);
    if (existing != null) {
      return existing;
    }

    Map<String, String> loaded = loadTable(langCode);
    if (loaded != null) {
      languages.put(langCode, Collections.unmodifiableMap(loaded));
    }
    return loaded;
  }

  private Map<String, String> loadTable(String langCode) {
    String resourcePath = "/lang/" + langCode + ".json";
    try {
      JSONObject json = readJsonObject(LanguageRegistry.class, resourcePath);
      Map<String, String> table = new LinkedHashMap<>(json.size());
      for (String key : json.keySet()) {
        table.put(key, json.getString(key));
      }
      log.info("[LanguageRegistry] Loaded " + table.size() + " entries for '" + langCode + "'");
      return table;
    } catch (Exception e) {
      log.warn("[LanguageRegistry] Could not load language '{}' from {}", langCode, resourcePath,
          e);
      return null;
    }
  }
}