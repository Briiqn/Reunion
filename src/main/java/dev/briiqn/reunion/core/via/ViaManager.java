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

import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.commands.ViaCommandHandler;
import dev.briiqn.reunion.core.config.Config;
import dev.briiqn.reunion.core.via.platforms.ReunionViaBackwardsPlatform;
import dev.briiqn.reunion.core.via.platforms.ReunionViaPlatform;
import dev.briiqn.reunion.core.via.platforms.ReunionViaRewindPlatform;
import java.io.File;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ViaManager {

  private static boolean initialized = false;

  private ViaManager() {
  }

  public static void init(Config config) {
    if (initialized || !config.getVia().isEnabled()) {
      return;
    }

    try {
      final File vvDir = new File("ViaVersion").getAbsoluteFile();

      if (!vvDir.exists()) {
        vvDir.mkdirs();
      }

      ViaManagerImpl.initAndLoad(
          new ReunionViaPlatform(vvDir),
          new ReunionViaInjector(),
          new ViaCommandHandler(false),
          new ReunionViaLoader(config),
          () -> {
            try {
              new ReunionViaBackwardsPlatform();
            } catch (Exception e) {
              log.error("Failed to load ViaBackwards", e);
            }

            try {
              new ReunionViaRewindPlatform();
            } catch (Exception e) {
              log.error("Failed to load ViaRewind", e);
            }
          }
      );
      initialized = true;
    } catch (Exception e) {
      log.error("Failed to initialize Via", e);
    }
  }

  public static boolean isEnabled() {
    return initialized && Via.isLoaded();
  }
}