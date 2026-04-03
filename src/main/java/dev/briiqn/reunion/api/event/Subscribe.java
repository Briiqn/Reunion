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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler.
 *
 * <p>The annotated method must be public, take exactly one parameter (the event type),
 * and return void. The method's owning class must be registered via
 * {@link EventBus#register(Object, Object)}.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyListener {
 *
 *     @Subscribe
 *     public void onJoin(PlayerJoinEvent event) {
 *         System.out.println(event.player().username() + " joined!");
 *     }
 *
 *     @Subscribe(priority = EventPriority.HIGH)
 *     public void onPreLogin(PreLoginEvent event) {
 *         if (isBanned(event.username())) {
 *             event.deny("You are banned.");
 *         }
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

  /**
   * The priority at which this handler is called. Handlers are invoked from
   * {@link EventPriority#LOWEST} to {@link EventPriority#MONITOR}. Default is
   * {@link EventPriority#NORMAL}.
   */
  EventPriority priority() default EventPriority.NORMAL;

  /**
   * If {@code true}, this handler will receive events even if they have been cancelled. Default is
   * {@code false} cancelled events are skipped.
   */
  boolean ignoreCancelled() default false;
}
