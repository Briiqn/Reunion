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

package dev.briiqn.reunion.core.util.auth;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class HWIDUtil {

  public static String getMac() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      NetworkInterface iface = NetworkInterface.getByInetAddress(addr);
      if (iface != null) {
        byte[] mac = iface.getHardwareAddress();
        if (mac != null) {
          return formatMac(mac);
        }
      }
      for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (ni.isLoopback() || ni.isVirtual()) {
          continue;
        }
        byte[] mac = ni.getHardwareAddress();
        if (mac == null) {
          continue;
        }
        return formatMac(mac);
      }
    } catch (Exception ignored) {
    }
    return "NOMAC";
  }

  private static String formatMac(byte[] mac) {
    StringBuilder sb = new StringBuilder();
    for (byte b : mac) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString();
  }

  public static String getCpuFingerprint() {
    OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    return System.getProperty("os.arch", "")
        + System.getProperty("os.name", "")
        + Runtime.getRuntime().availableProcessors()
        + os.getTotalMemorySize();
  }

  public static char[] buildPass() {
    return (getMac() + "|" + getCpuFingerprint()).toCharArray();
  }
}