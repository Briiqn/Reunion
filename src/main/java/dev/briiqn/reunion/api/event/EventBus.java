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

/**
 * The central event bus for Reunion.
 *
 * <p>Listeners are POJO objects with methods annotated with {@link Subscribe}.
 * Registering a listener scans all public methods on the object for {@code @Subscribe} annotations
 * and wires them up automatically.
 *
 * <p>All listener registrations are associated with a plugin instance so they can
 * be bulk-unregistered when the plugin is disabled.
 *
 * <p>Example  registering a listener class:
 * <pre>{@code
 * proxy().eventBus().register(this, new MyListener());
 * }</pre>
 *
 * <p>Example  registering a lambda handler directly:
 * <pre>{@code
 * proxy().eventBus().register(this, PlayerJoinEvent.class, event -> {
 *     event.player().sendMessage("Welcome!");
 * });
 * }</pre>
 */
public interface EventBus {

  /**
   * Registers all {@link Subscribe}-annotated methods on the given listener object.
   *
   * @param plugin   the owning plugin (used for cleanup on unload)
   * @param listener the listener object whose methods to register
   */
  void register(Plugin plugin, Object listener);

  /**
   * Registers a single functional handler for an event type.
   *
   * @param plugin  the owning plugin
   * @param type    the event class to listen for
   * @param handler the handler to invoke
   * @param <E>     the event type
   */
  <E extends Event> void register(Plugin plugin, Class<E> type, EventHandler<E> handler);

  /**
   * Registers a single functional handler for an event type with a specific priority.
   *
   * @param plugin   the owning plugin
   * @param type     the event class to listen for
   * @param priority the invocation priority
   * @param handler  the handler to invoke
   * @param <E>      the event type
   */
  <E extends Event> void register(Plugin plugin, Class<E> type, EventPriority priority,
      EventHandler<E> handler);

  /**
   * Unregisters a specific listener object. All methods on the object that were previously
   * registered are removed.
   *
   * @param listener the listener object to unregister
   */
  void unregister(Object listener);

  /**
   * Unregisters all listeners registered by the given plugin. Called automatically when a plugin is
   * disabled.
   *
   * @param plugin the plugin whose listeners to remove
   */
  void unregisterAll(Plugin plugin);

  /**
   * Fires an event synchronously, invoking all registered handlers in priority order.
   *
   * <p>Returns the same event object after all handlers have run, so callers can
   * inspect the final state (e.g. whether it was cancelled).
   *
   * @param event the event to fire
   * @param <E>   the event type
   * @return the event, post-processing
   */
  <E extends Event> E fire(E event);

  /**
   * Fires an event and checks whether it was allowed to proceed.
   *
   * <p>Only works with {@link ResultedEvent}. For other event types this always
   * returns {@code true}.
   *
   * @param event the event to fire
   * @param <E>   the event type
   * @return {@code true} if the event was not denied
   */
  <E extends ResultedEvent> boolean fireAndCheck(E event);


  /**
   * A functional interface for inline event handler registration.
   *
   * @param <E> the event type
   */
  @FunctionalInterface
  interface EventHandler<E extends Event> {

    void handle(E event);
  }
}
