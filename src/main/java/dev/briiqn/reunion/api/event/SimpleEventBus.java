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

package dev.briiqn.reunion.api.event;

import dev.briiqn.reunion.api.plugin.Plugin;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * an implementation of {@link EventBus} based on <a
 * href="https://github.com/Briiqn/base-utils">...</a>.
 */
public final class SimpleEventBus implements EventBus {

  private static final Logger log = LogManager.getLogger(SimpleEventBus.class);

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  private static final MethodType HANDLER_TYPE =
      MethodType.methodType(void.class, Object.class, Object.class);

  private static final VarHandle CANCELLED_VH;

  static {
    try {
      MethodHandles.Lookup cancelledLookup =
          MethodHandles.privateLookupIn(CancellableEvent.class, LOOKUP);
      CANCELLED_VH = cancelledLookup.findVarHandle(
          CancellableEvent.class, "cancelled", boolean.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new ExceptionInInitializerError(
          "Cannot obtain VarHandle for CancellableEvent.cancelled: " + e);
    }
  }


  private final Map<Class<? extends Event>, List<Registration>> handlers =
      new ConcurrentHashMap<>();

  private final Map<Plugin, List<Registration>> pluginRegistrations =
      new ConcurrentHashMap<>();

  private final Map<Object, List<Registration>> listenerRegistrations =
      new ConcurrentHashMap<>();


  @Override
  public void register(Plugin plugin, Object listener) {
    List<Registration> registered = new ArrayList<>();
    Class<?> listenerClass = listener.getClass();

    MethodHandles.Lookup listenerLookup;
    try {
      listenerLookup = MethodHandles.privateLookupIn(listenerClass, LOOKUP);
    } catch (IllegalAccessException e) {
      listenerLookup = LOOKUP;
    }

    for (Method method : listenerClass.getMethods()) {
      Subscribe sub = method.getAnnotation(Subscribe.class);
      if (sub == null) {
        continue;
      }

      Class<?>[] params = method.getParameterTypes();
      if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
        log.warn(
            "[EventBus] Skipping {}.{}  @Subscribe method must take exactly one Event subtype.",
            listenerClass.getSimpleName(), method.getName());
        continue;
      }

      @SuppressWarnings("unchecked")
      Class<? extends Event> eventType = (Class<? extends Event>) params[0];

      MethodHandle mh;
      try {
        mh = listenerLookup.unreflect(method)
            .asType(HANDLER_TYPE);
      } catch (IllegalAccessException e) {
        log.error("[EventBus] Cannot access handler {}.{}: {}",
            listenerClass.getSimpleName(), method.getName(), e.getMessage());
        continue;
      }

      MethodHandle bound = mh.bindTo(listener);
      Registration reg = new Registration(
          plugin, listener, eventType,
          sub.priority(), sub.ignoreCancelled(),
          bound
      );

      addToHandlers(eventType, reg);
      registered.add(reg);
    }

    if (!registered.isEmpty()) {
      pluginRegistrations
          .computeIfAbsent(plugin, k -> new CopyOnWriteArrayList<>())
          .addAll(registered);
      listenerRegistrations.put(listener, registered);
    }
  }

  @Override
  public <E extends Event> void register(Plugin plugin, Class<E> type, EventHandler<E> handler) {
    register(plugin, type, EventPriority.NORMAL, handler);
  }

  @Override
  public <E extends Event> void register(Plugin plugin, Class<E> type,
      EventPriority priority, EventHandler<E> handler) {
    MethodHandle mh;
    try {
      Method handleMethod = EventHandler.class.getMethod("handle", Event.class);
      mh = LOOKUP.unreflect(handleMethod)
          .asType(HANDLER_TYPE)
          .bindTo(handler);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalStateException("Cannot create MH for EventHandler.handle", e);
    }

    Registration reg = new Registration(plugin, null, type, priority, false, mh);
    addToHandlers(type, reg);
    pluginRegistrations
        .computeIfAbsent(plugin, k -> new CopyOnWriteArrayList<>())
        .add(reg);
  }


  @Override
  public void unregister(Object listener) {
    List<Registration> regs = listenerRegistrations.remove(listener);
    if (regs == null) {
      return;
    }
    for (Registration reg : regs) {
      List<Registration> bucket = handlers.get(reg.eventType);
      if (bucket != null) {
        bucket.remove(reg);
      }
    }
    for (List<Registration> owned : pluginRegistrations.values()) {
      owned.removeAll(regs);
    }
  }

  @Override
  public void unregisterAll(Plugin plugin) {
    List<Registration> regs = pluginRegistrations.remove(plugin);
    if (regs == null) {
      return;
    }
    for (Registration reg : regs) {
      List<Registration> bucket = handlers.get(reg.eventType);
      if (bucket != null) {
        bucket.remove(reg);
      }
      if (reg.listener != null) {
        listenerRegistrations.remove(reg.listener);
      }
    }
  }


  @Override
  public <E extends Event> E fire(E event) {
    List<Registration> bucket = handlers.get(event.getClass());
    if (bucket == null || bucket.isEmpty()) {
      return event;
    }

    final boolean isCancellable = event instanceof CancellableEvent;
    boolean cancelled = isCancellable && (boolean) CANCELLED_VH.get(event);

    for (int i = 0, size = bucket.size(); i < size; i++) {
      Registration reg = bucket.get(i);

      if (cancelled && !reg.ignoreCancelled) {
        continue;
      }

      try {
        reg.handle.invokeExact((Object) event);
      } catch (Throwable t) {
        log.error("[EventBus] Unhandled exception in handler for {}",
            event.getClass().getSimpleName(), t);
      }

      if (isCancellable) {
        cancelled = (boolean) CANCELLED_VH.get(event);
      }
    }

    return event;
  }

  @Override
  public <E extends ResultedEvent> boolean fireAndCheck(E event) {
    fire(event);
    return event.isAllowed();
  }


  private void addToHandlers(Class<? extends Event> type, Registration reg) {
    handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(reg);
    handlers.get(type).sort(Comparator.comparingInt(r -> r.priority.order()));
  }

  private record Registration(Plugin plugin, Object listener, Class<? extends Event> eventType,
                              EventPriority priority, boolean ignoreCancelled,
                              MethodHandle handle) {

  }
}