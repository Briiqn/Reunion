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

package dev.briiqn.reunion.core.util.game;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import lombok.extern.log4j.Log4j2;

/**
 * Mirrors the C++ skin bitmask macros and getSkinPathFromId / getCapePathFromId from Player.cpp so
 * the proxy can convert the DWORD skin IDs the LCE client declares in its login packet into the
 * filename strings used by the texture relay.
 * <p>
 * Bitmask layout (treated as unsigned 32-bit): bit  31      = DLC flag
 * (GET_IS_DLC_SKIN_FROM_BITMASK) bits 0–30    = DLC skin ID     when bit 31 is set
 * (GET_DLC_SKIN_ID_FROM_BITMASK) bits 5–30    = UGC skin ID     when bit 31 is clear
 * (GET_UGC_SKIN_ID_FROM_BITMASK, mask 0x7FFFFFE0) bits 0–4     = default index   when bit 31 clear
 * and UGC bits are zero (GET_DEFAULT_SKIN_ID_FROM_BITMASK, mask 0x1F)
 * <p>
 * Filename formats (from C++ getSkinPathFromId / getCapePathFromId): DLC skin: "dlcskin%08d.png"
 * (decimal, bits 0-30 as ID) UGC skin:     "ugcskin%08X.png"  (uppercase hex, bits 5-30 as ID)
 * Default skin: null               (built-in, no relay needed) No skin (0):  null
 */
@Log4j2
public final class SkinUtil {

  private SkinUtil() {
  }

  /**
   * Converts an LCE skin DWORD ID (as sent in the login packet) to the filename string that
   * ::getSkinPathFromId produces
   */
  public static String getSkinPathFromId(int skinId) {
    if (skinId == 0) {
      return null;
    }
    // ints are signed in java, we need it unsigned.
    long id = Integer.toUnsignedLong(skinId);

    if ((id & 0x80000000L) != 0) {
      // DLC skin decimal filename, bits 0-30 as the DLC skin ID
      long dlcId = id & 0x7FFFFFFFL;
      return String.format("dlcskin%08d.png", dlcId);
    }

    long ugcId = id & 0x7FFFFFE0L; // bits 5-30
    long defaultId = id & 0x1FL;        // bits 0-4

    if (ugcId != 0) {
      // UGC (user) skin, uppercase hex filename
      return String.format("ugcskin%08X.png", ugcId);
    }
    if (defaultId != 0) {
      return null;
    }
    return null; // skinId == 0
  }

  public static String getCapePathFromId(int capeId) {
    if (capeId == 0) {
      return null;
    }
    long id = Integer.toUnsignedLong(capeId);

    if ((id & 0x80000000L) != 0) {
      long dlcId = id & 0x7FFFFFFFL;
      return String.format("dlccape%08d.png", dlcId);
    }

    long ugcId = id & 0x7FFFFFE0L;
    long defaultId = id & 0x1FL;

    if (ugcId != 0) {
      return String.format("ugccape%08X.png", ugcId);
    }
    if (defaultId != 0) {
      return null;
    }
    return null;
  }

  public static byte[] convertLceToJava(byte[] skinBytes) {
    try {
      BufferedImage src = ImageIO.read(new ByteArrayInputStream(skinBytes));
      if (src == null || src.getWidth() != 64 || src.getHeight() != 32) {
        return skinBytes;
      }

      BufferedImage javaSkin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = javaSkin.createGraphics();
      g.drawImage(src, 0, 0, null);
      g.drawImage(src.getSubimage(0, 16, 16, 16), 16, 48, null);
      g.drawImage(src.getSubimage(40, 16, 16, 16), 32, 48, null);
      g.dispose();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(javaSkin, "PNG", baos);
      return baos.toByteArray();
    } catch (Exception e) {
      return skinBytes;
    }
  }

  public static byte[] convertJavaToLCE(byte[] skinBytes) {
    try {
      BufferedImage src = ImageIO.read(new ByteArrayInputStream(skinBytes));
      if (src == null) {
        return skinBytes;
      }

      if (src.getWidth() == 64 && src.getHeight() == 32) {
        return skinBytes;
      }
      if (src.getWidth() != 64 || src.getHeight() != 64) {
        return skinBytes;
      }

      BufferedImage lce = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = lce.createGraphics();
      g.setComposite(AlphaComposite.Src);
      g.drawImage(src.getSubimage(0, 0, 64, 32), 0, 0, null);

      g.setComposite(AlphaComposite.SrcOver);
      g.drawImage(src.getSubimage(0, 32, 16, 16), 0, 16, null);
      g.drawImage(src.getSubimage(16, 32, 24, 16), 16, 16, null);
      g.drawImage(src.getSubimage(40, 32, 16, 16), 40, 16, null);
      g.drawImage(src.getSubimage(0, 48, 16, 16), 0, 16, null);
      g.drawImage(src.getSubimage(48, 48, 16, 16), 40, 16, null);
      g.dispose();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(lce, "PNG", baos);
      return baos.toByteArray();

    } catch (Exception e) {
      log.warn("Failed to convert skin to LCE format, sending raw: {}", e.getMessage());
      return skinBytes;
    }
  }

  public static byte[] adaptSlimToFull(byte[] lce64x32png) {
    try {
      java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(
          new java.io.ByteArrayInputStream(lce64x32png));

      if (src == null || src.getWidth() != 64 || src.getHeight() != 32) {
        return lce64x32png;
      }

      java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(
          64, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);

      for (int y = 0; y < 32; y++) {
        for (int x = 0; x < 64; x++) {
          dst.setRGB(x, y, src.getRGB(x, y));
        }
      }

      for (int y = 16; y < 20; y++) {
        int t0 = src.getRGB(44, y);
        int t1 = src.getRGB(45, y);
        int t2 = src.getRGB(46, y);
        dst.setRGB(44, y, t0);
        dst.setRGB(45, y, t1);
        dst.setRGB(46, y, t2);
        dst.setRGB(47, y, t2);

        int b0 = src.getRGB(47, y);
        int b1 = src.getRGB(48, y);
        int b2 = src.getRGB(49, y);
        dst.setRGB(47, y, t2);
        // BUT bottom cap needs to  to x=48..51
        dst.setRGB(48, y, b0);
        dst.setRGB(49, y, b1);
        dst.setRGB(50, y, b2);
        dst.setRGB(51, y, b2);
      }

      for (int y = 20; y < 32; y++) {

        int f0 = src.getRGB(44, y);
        int f1 = src.getRGB(45, y);
        int f2 = src.getRGB(46, y);
        dst.setRGB(44, y, f0);
        dst.setRGB(45, y, f1);
        dst.setRGB(46, y, f2);
        dst.setRGB(47, y, f2); // expand front face to 4 wide

        // Left side: x=47..50 → x=48..51 (shift right by 1, duplicate last)
        int l0 = src.getRGB(47, y);
        int l1 = src.getRGB(48, y);
        int l2 = src.getRGB(49, y);
        int l3 = src.getRGB(50, y);
        dst.setRGB(48, y, l0);
        dst.setRGB(49, y, l1);
        dst.setRGB(50, y, l2);
        dst.setRGB(51, y, l3);

        // Back face: x=51..53 → x=52..55 (shift right by 1, duplicate last)
        int k0 = src.getRGB(51, y);
        int k1 = src.getRGB(52, y);
        int k2 = src.getRGB(53, y);
        dst.setRGB(52, y, k0);
        dst.setRGB(53, y, k1);
        dst.setRGB(54, y, k2);
        dst.setRGB(55, y, k2); // expand back face to 4 wide
      }

      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      javax.imageio.ImageIO.write(dst, "PNG", baos);
      return baos.toByteArray();

    } catch (Exception e) {
      log.warn("adaptSlimSkinToFullArm failed: {}", e.getMessage());
      return lce64x32png;
    }
  }
}