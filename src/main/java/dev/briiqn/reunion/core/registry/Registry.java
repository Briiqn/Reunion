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

package dev.briiqn.reunion.core.registry;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class Registry<T> {

  protected static JSONArray readJsonArray(Class<?> caller, String resource) throws Exception {
    try (InputStream in = caller.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Resource not found: " + resource);
      }
      return JSON.parseArray(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  protected static JSONObject readJsonObject(Class<?> caller, String resource) throws Exception {
    try (InputStream in = caller.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("Resource not found: " + resource);
      }
      return JSON.parseObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  public abstract T get(int id);

  public abstract boolean isUnsupported(int id);

  public abstract int remap(int id);
}