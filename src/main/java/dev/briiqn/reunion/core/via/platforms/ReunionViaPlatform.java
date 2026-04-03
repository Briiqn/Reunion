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

package dev.briiqn.reunion.core.via.platforms;

import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import dev.briiqn.reunion.core.ReunionServer;
import java.io.File;
import java.util.logging.Logger;

public final class ReunionViaPlatform extends UserConnectionViaVersionPlatform {

  public ReunionViaPlatform(File dataFolder) {
    super(dataFolder);
  }

  @Override
  public Logger createLogger(String name) {
    return Logger.getLogger(name);
  }

  @Override
  public boolean isProxy() {
    return true;
  }

  @Override
  public String getPlatformName() {
    return "Reunion";
  }

  @Override
  public String getPlatformVersion() {
    return ReunionServer.releaseVersion;
  }
}