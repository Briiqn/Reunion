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

package dev.briiqn.reunion.core.util.math.vector;


import lombok.Getter;

public final class SoundVec3 extends Vec3i {

  @Getter
  private final int soundId;


  public SoundVec3(int x, int y, int z, int soundId) {
    super(x, y, z);
    this.soundId = soundId;
  }


  public SoundVec3 withSound(int newSoundId) {
    return new SoundVec3(x(), y(), z(), newSoundId);
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    if (!(o instanceof SoundVec3 s)) {
      return false;
    }
    return soundId == s.soundId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + soundId;
  }

  @Override
  public String toString() {
    return "SoundVec3{" + x() + ", " + y() + ", " + z() + ", sound=" + soundId + "}";
  }
}