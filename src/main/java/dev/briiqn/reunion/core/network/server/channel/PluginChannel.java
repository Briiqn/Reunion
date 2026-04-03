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

package dev.briiqn.reunion.core.network.server.channel;

import dev.briiqn.reunion.core.session.ConsoleSession;
import java.util.Set;
import lombok.Getter;

@Getter
public abstract class PluginChannel {

  private final Set<String> channels;

  protected PluginChannel(String... channels) {
    this.channels = Set.of(channels);
  }

  public boolean supports(String channel) {
    return channels.contains(channel);
  }

  public void handleIncoming(ConsoleSession session, String channel, byte[] data) {
  }

  public void onJoin(ConsoleSession session) {
  }
}