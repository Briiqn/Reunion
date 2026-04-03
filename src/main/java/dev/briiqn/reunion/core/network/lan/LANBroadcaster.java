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

package dev.briiqn.reunion.core.network.lan;


import io.netty.channel.epoll.Epoll;
import lombok.Getter;

@Getter
public abstract class LANBroadcaster {

  protected final BroadcastMode mode;

  protected LANBroadcaster(BroadcastMode mode) {
    this.mode = mode;
  }

  public abstract void start() throws Exception;

  public abstract void stop();

  public abstract void broadcast();

  public boolean epollAvailable() {
    return Epoll.isAvailable();
  }
}
