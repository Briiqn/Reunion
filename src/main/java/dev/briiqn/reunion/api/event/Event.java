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

/**
 * Base marker interface for all Reunion events.
 *
 * <p>Events are fired via {@link EventBus#fire(Event)} and listened to
 * using methods annotated with {@link Subscribe} on a registered listener class.
 *
 * <p>Events that can be cancelled implement {@link CancellableEvent}.
 * Events that carry a result (e.g. allow/deny) implement {@link ResultedEvent}.
 */
public interface Event {

}
