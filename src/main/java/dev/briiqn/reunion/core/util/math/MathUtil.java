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

package dev.briiqn.reunion.core.util.math;

public final class MathUtil {

  public static float sin(float value) {
    return (float) Math.sin(value);
  }

  public static float cos(float value) {
    return (float) Math.cos(value);
  }

  public static float sqrt(float value) {
    return (float) Math.sqrt(value);
  }

  public static double sqrt(double value) {
    return Math.sqrt(value);
  }

  public static int floor(double value) {
    int i = (int) value;
    return value < (double) i ? i - 1 : i;
  }

  public static int ceil(double value) {
    int i = (int) value;
    return value > (double) i ? i + 1 : i;
  }

  public static float clamp(float value, float min, float max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static double clamp(double value, double min, double max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static int clamp(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static float wrapAngleTo180(float value) {
    value %= 360.0F;
    if (value >= 180.0F) {
      value -= 360.0F;
    }
    if (value < -180.0F) {
      value += 360.0F;
    }
    return value;
  }

  public static double wrapAngleTo180(double value) {
    value %= 360.0D;
    if (value >= 180.0D) {
      value -= 360.0D;
    }
    if (value < -180.0D) {
      value += 360.0D;
    }
    return value;
  }

  public static double max(double a, double b, double c) {
    return Math.max(a, Math.max(b, c));
  }
}