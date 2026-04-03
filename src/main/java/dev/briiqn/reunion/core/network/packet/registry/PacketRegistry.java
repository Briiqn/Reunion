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

package dev.briiqn.reunion.core.network.packet.registry;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.Packet;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class PacketRegistry {

  private static final Map<Class<?>, PacketInfo> INFO_CACHE = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<Integer, MethodHandle>> READ_METHODS = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<Integer, MethodHandle>> WRITE_METHODS = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Map<Integer, MethodHandle>> HANDLE_METHODS = new ConcurrentHashMap<>();
  private static final Pattern READ_PATTERN = Pattern.compile("read(\\d+)");
  private static final Pattern WRITE_PATTERN = Pattern.compile("write(\\d+)");
  private static final Pattern HANDLE_PATTERN = Pattern.compile("handle(\\d+)");
  private final Map<Key, MethodHandle> registry = new HashMap<>();

  public static PacketInfo getPacketInfo(Class<?> clazz) {
    return INFO_CACHE.computeIfAbsent(clazz, c -> c.getAnnotation(PacketInfo.class));
  }

  public static void invokeRead(Packet packet, ByteBuf buf, int protocol) {
    try {
      Map<Integer, MethodHandle> map = READ_METHODS.get(packet.getClass());
      if (map != null) {
        MethodHandle mh = map.get(protocol);
        if (mh != null) {
          mh.invokeExact(packet, buf);
          return;
        }
      }
      packet.read(buf);
    } catch (Throwable t) {
      throw new RuntimeException(
          "Error invoking read for protocol " + protocol + " on " + packet.getClass()
              .getSimpleName(), t);
    }


  }

  public static void invokeWrite(Packet packet, ByteBuf buf, int protocol) {
    try {
      Map<Integer, MethodHandle> map = WRITE_METHODS.get(packet.getClass());
      if (map != null) {
        MethodHandle mh = map.get(protocol);
        if (mh != null) {
          mh.invokeExact(packet, buf);
          return;
        }
      }
      packet.write(buf);
    } catch (Throwable t) {
      throw new RuntimeException(
          "Error invoking write for protocol " + protocol + " on " + packet.getClass()
              .getSimpleName(), t);
    }
  }

  public static void invokeHandle(Packet packet, Object session, int protocol) {
    try {
      Map<Integer, MethodHandle> map = HANDLE_METHODS.get(packet.getClass());
      if (map != null) {
        MethodHandle mh = map.get(protocol);
        if (mh != null) {
          mh.invokeExact(packet, session);
          return;
        }
      }
      if (packet instanceof ConsoleC2SPacket c2s) {
        c2s.handle((ConsoleSession) session);
      } else if (packet instanceof JavaS2CPacket s2c) {
        s2c.handle((JavaSession) session);
      }
    } catch (Throwable t) {
      throw new RuntimeException(
          "Error invoking handle for protocol " + protocol + " on " + packet.getClass()
              .getSimpleName(), t);

    }

  }

  public void scan(String basePackage) {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodType readWriteType = MethodType.methodType(void.class, Packet.class, ByteBuf.class);
    MethodType handleType = MethodType.methodType(void.class, Packet.class, Object.class);

    try (ScanResult result = new ClassGraph()
        .enableClassInfo()
        .enableAnnotationInfo()
        .acceptPackages(basePackage)
        .scan()) {

      for (ClassInfo ci : result.getClassesWithAnnotation(PacketInfo.class.getName())) {
        Class<?> clazz = ci.loadClass();
        PacketInfo info = clazz.getAnnotation(PacketInfo.class);
        if (info == null) {
          continue;
        }

        INFO_CACHE.put(clazz, info);

        try {
          MethodHandle ctor = MethodHandles.privateLookupIn(clazz, lookup)
              .findConstructor(clazz, MethodType.methodType(void.class));
          Key key = new Key(info.side(), info.id());
          registry.put(key, ctor);

          Map<Integer, MethodHandle> readMap = new HashMap<>();
          Map<Integer, MethodHandle> writeMap = new HashMap<>();
          Map<Integer, MethodHandle> handleMap = new HashMap<>();

          for (Method m : clazz.getDeclaredMethods()) {
            m.setAccessible(true);
            String name = m.getName();

            Matcher rm = READ_PATTERN.matcher(name);
            if (rm.matches() && m.getParameterCount() == 1
                && m.getParameterTypes()[0] == ByteBuf.class) {
              readMap.put(Integer.parseInt(rm.group(1)), lookup.unreflect(m).asType(readWriteType));
            }

            Matcher wm = WRITE_PATTERN.matcher(name);
            if (wm.matches() && m.getParameterCount() == 1
                && m.getParameterTypes()[0] == ByteBuf.class) {
              writeMap.put(Integer.parseInt(wm.group(1)),
                  lookup.unreflect(m).asType(readWriteType));
            }

            Matcher hm = HANDLE_PATTERN.matcher(name);
            if (hm.matches()
                && m.getParameterCount() == 1) {
              handleMap.put(Integer.parseInt(hm.group(1)), lookup.unreflect(m).asType(handleType));
            }
          }

          if (!readMap.isEmpty()) {
            READ_METHODS.put(clazz, readMap);
          }
          if (!writeMap.isEmpty()) {
            WRITE_METHODS.put(clazz, writeMap);
          }
          if (!handleMap.isEmpty()) {
            HANDLE_METHODS.put(clazz, handleMap);
          }

          log.debug("Registered {} side={} id=0x{}",
              clazz.getSimpleName(), info.side(), String.format("%02X", info.id()));
        } catch (Exception e) {
          log.error("[PacketRegistry] Failed to register {}: {}",
              clazz.getSimpleName(), e.getMessage());
        }
      }
    }

    log.debug("[PacketRegistry] {} packets registered.", registry.size());
  }

  public Packet create(PacketSide side, int id) {
    MethodHandle mh = registry.get(new Key(side, id));
    if (mh == null) {
      return null;
    }
    try {
      return (Packet) mh.invoke();
    } catch (Throwable t) {
      throw new RuntimeException("Failed to instantiate packet side=" + side + " id=" + id, t);
    }
  }

  public boolean has(PacketSide side, int id) {
    return registry.containsKey(new Key(side, id));
  }

  private record Key(PacketSide side, int id) {

  }
}