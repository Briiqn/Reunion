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

package dev.briiqn.reunion.core.manager;

import dev.briiqn.reunion.core.network.server.channel.PluginChannel;
import dev.briiqn.reunion.core.network.server.channel.impl.BungeeCordChannel;
import dev.briiqn.reunion.core.network.server.channel.impl.ReunionInfoChannel;
import dev.briiqn.reunion.core.network.server.channel.impl.ViaVersionChannel;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.util.ArrayList;
import java.util.List;

public final class ChannelManager {

  private static final List<PluginChannel> channels = new ArrayList<>();

  static {
    channels.add(new BungeeCordChannel());
    channels.add(new ViaVersionChannel());
    channels.add(new ReunionInfoChannel());
  }

  private ChannelManager() {
  }

  public static void handleIncoming(ConsoleSession session, String channel, byte[] data) {
    for (PluginChannel c : channels) {
      if (c.supports(channel)) {
        c.handleIncoming(session, channel, data);
        return;
      }
    }
  }

  public static boolean isSupported(String channel) {
    for (PluginChannel c : channels) {
      if (c.supports(channel)) {
        return true;
      }
    }
    return false;
  }

  public static void onJoin(ConsoleSession session) {
    for (PluginChannel c : channels) {
      c.onJoin(session);
    }
  }

  public static String[] getRegisteredChannels() {
    return channels.stream()
        .flatMap(c -> c.getChannels().stream())
        .toArray(String[]::new);
  }
}