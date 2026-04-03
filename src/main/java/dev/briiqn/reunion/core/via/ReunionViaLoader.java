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
package dev.briiqn.reunion.core.via;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocol.version.BaseVersionProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.CompressionProvider;
import dev.briiqn.reunion.core.config.Config;
import dev.briiqn.reunion.core.network.pipeline.java.decode.JavaViaDecompressor;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaViaCompressor;
import dev.briiqn.reunion.core.network.server.ServerSwitchHandler;
import dev.briiqn.reunion.core.util.game.ServerPinger;
import io.netty.channel.Channel;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ReunionViaLoader implements ViaPlatformLoader {

  private final Config config;

  public ReunionViaLoader(Config config) {
    this.config = config;
  }

  private static void install(Channel ch, int threshold) {
    ch.eventLoop().execute(() -> {
      if (ch.pipeline().get("decompressor") == null) {
        ch.pipeline().addBefore("via-codec", "decompressor", new JavaViaDecompressor(threshold));
      }
      if (ch.pipeline().get("compressor") == null) {
        ch.pipeline().addAfter("length-encoder", "compressor", new JavaViaCompressor(threshold));
      }
    });
  }

  @Override
  public void load() {
    ProtocolVersion fallback = resolveTargetVersion();

    Via.getManager().getProviders().use(VersionProvider.class, new BaseVersionProvider() {
      @Override
      public ProtocolVersion getClosestServerProtocol(UserConnection connection) {
        Channel ch = connection.getChannel();
        if (ch != null) {
          ProtocolVersion ver = ch.attr(ServerSwitchHandler.BACKEND_VERSION).get();
          if (ver != null) {
            return ver;
          }
        }
        return fallback;
      }
    });

    Via.getManager().getProviders().use(CompressionProvider.class, new CompressionProvider() {
      @Override
      public void handlePlayCompression(UserConnection user, int threshold) {
        install(Objects.requireNonNull(user.getChannel()), threshold);
      }
    });
  }

  @Override
  public void unload() {
  }

  private ProtocolVersion resolveTargetVersion() {
    String targetVersionStr = config.getVia().getTargetVersion();

    if ("auto".equalsIgnoreCase(targetVersionStr)) {
      return resolveAuto();
    }

    ProtocolVersion target = ProtocolVersion.getClosest(targetVersionStr);
    if (target == null) {
      log.warn("[Via] Protocol {} not recognised, falling back", targetVersionStr);
      return ProtocolVersion.v26_1;
    }
    return target;
  }

  private ProtocolVersion resolveAuto() {
    String host = config.getConnection().getJavaHost();
    int port = config.getConnection().getJavaPort();

    ProtocolVersion detected = ServerPinger.detect(host, port);
    if (detected == null) {
      log.warn("[Via] Could not detect protocol for {}:{}, falling back", host, port);
      return ProtocolVersion.v26_1;
    }

    log.info("[Via] Detected {} ({}) for default backend.", detected.getName(),
        detected.getVersion());
    return detected;
  }
}