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

package dev.briiqn.reunion.api.event.player;

import dev.briiqn.reunion.api.event.ResultedEvent;
import java.net.InetSocketAddress;

/**
 * Fired when a console client initiates a connection, before any authentication or login processing
 * has occurred.
 *
 * <p>Three outcomes are possible:
 * <ul>
 *   <li><b>Allow (default)</b>  {@link #allow()}  proceed to Java backend connection.</li>
 *   <li><b>Deny</b>  {@link #deny(String)}  disconnect the client with a reason.</li>
 *   <li><b>Hold</b>  {@link #hold()}  keep the console connection open but do NOT
 *       connect to a Java backend. The plugin is responsible for sending fake world
 *       packets and later calling {@code player.connectTo("server")} when ready.
 *       This is how a limbo implementation works.</li>
 * </ul>
 *
 * <pre>{@code
 *  * @Subscribe
 * public void onPreLogin(PreLoginEvent event) {
 *     event.hold();  * }
 * }</pre>
 */
public final class PreLoginEvent extends ResultedEvent {

  private final String username;
  private final InetSocketAddress remoteAddress;
  private final int clientVersion;

  public PreLoginEvent(String username, InetSocketAddress remoteAddress, int clientVersion) {
    this.username = username;
    this.remoteAddress = remoteAddress;
    this.clientVersion = clientVersion;
  }

  /**
   * The username sent by the client. Not yet verified.
   */
  public String username() {
    return username;
  }

  /**
   * The client's remote address.
   */
  public InetSocketAddress remoteAddress() {
    return remoteAddress;
  }

  /**
   * The client's protocol version number. Known values: {@code 39} (older LCE), {@code 78} (newer
   * LCE).
   */
  public int clientVersion() {
    return clientVersion;
  }
}