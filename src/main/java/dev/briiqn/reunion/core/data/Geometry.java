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

package dev.briiqn.reunion.core.data;


public record Geometry(int dwSkinID, byte[] textureData, int animOverride, byte[] boxData,
                       int boxCount) {

  public Geometry(int dwSkinID, byte[] textureData,
      int animOverride, byte[] boxData, int boxCount) {
    this.dwSkinID = dwSkinID;
    this.textureData = textureData != null ? textureData : new byte[0];
    this.animOverride = animOverride;
    this.boxData = boxData != null ? boxData : new byte[0];
    this.boxCount = boxCount;
  }


  public boolean hasGeometry() {
    return boxCount > 0;
  }
}