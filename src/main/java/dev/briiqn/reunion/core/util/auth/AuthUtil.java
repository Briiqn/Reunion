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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

public class AuthUtil {

  private static final byte[] MAGIC = {0x4D, 0x43, 0x41, 0x31};
  private static final byte VERSION = 0x01;
  private static final int SALT_LEN = 16;
  private static final int IV_LEN = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final int PBKDF2_ITERATIONS = 10_000;
  private static final Path SAVE_PATH = Path.of("saves", "account.bin");

  private static String getMacAddress() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      NetworkInterface iface = NetworkInterface.getByInetAddress(addr);
      if (iface != null) {
        byte[] mac = iface.getHardwareAddress();
        if (mac != null) {
          StringBuilder sb = new StringBuilder();
          for (byte b : mac) {
            sb.append(String.format("%02X", b));
          }
          return sb.toString();
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
        StringBuilder sb = new StringBuilder();
        for (byte b : mac) {
          sb.append(String.format("%02X", b));
        }
        return sb.toString();
      }
    } catch (Exception ignored) {
    }
    return "NOMAC";
  }

  private static String getCpuFingerprint() {
    com.sun.management.OperatingSystemMXBean os =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    return System.getProperty("os.arch", "")
        + System.getProperty("os.name", "")
        + Runtime.getRuntime().availableProcessors()
        + os.getTotalMemorySize();
  }

  private static char[] buildPass() {
    return (getMacAddress() + "|" + getCpuFingerprint()).toCharArray();
  }

  private static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    byte[] key = factory.generateSecret(new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, 256))
        .getEncoded();
    return new SecretKeySpec(key, "AES");
  }

  private static byte[] encrypt(SecretKey key, byte[] data) throws GeneralSecurityException {
    byte[] iv = new byte[IV_LEN];
    new SecureRandom().nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
    byte[] ct = cipher.doFinal(data);
    ByteBuffer buf = ByteBuffer.allocate(IV_LEN + ct.length);
    buf.put(iv);
    buf.put(ct);
    return buf.array();
  }

  private static byte[] decrypt(SecretKey key, byte[] blob) throws GeneralSecurityException {
    ByteBuffer buf = ByteBuffer.wrap(blob);
    byte[] iv = new byte[IV_LEN];
    byte[] ct = new byte[blob.length - IV_LEN];
    buf.get(iv);
    buf.get(ct);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
    return cipher.doFinal(ct);
  }

  public static void saveSession(JavaAuthManager authManager) throws Exception {
    JsonObject json = JavaAuthManager.toJson(authManager);
    byte[] payload = json.toString().getBytes(StandardCharsets.UTF_8);

    char[] password = buildPass();
    byte[] salt = new byte[SALT_LEN];
    new SecureRandom().nextBytes(salt);

    SecretKey key = deriveKey(password, salt);
    Arrays.fill(password, '\0');

    ByteBuffer inner = ByteBuffer.allocate(4 + payload.length);
    inner.putInt(payload.length);
    inner.put(payload);

    byte[] blob = encrypt(key, inner.array());

    Files.createDirectories(SAVE_PATH.getParent());
    try (DataOutputStream out = new DataOutputStream(
        new BufferedOutputStream(Files.newOutputStream(SAVE_PATH)))) {
      out.write(MAGIC);
      out.writeByte(VERSION);
      out.write(salt);
      out.writeShort(GCM_TAG_BITS);
      out.writeInt(blob.length);
      out.write(blob);
    }
  }

  public static JavaAuthManager loadSession(HttpClient httpClient) throws Exception {
    if (!Files.exists(SAVE_PATH)) {
      return null;
    }

    char[] password = buildPass();

    try (DataInputStream in = new DataInputStream(
        new BufferedInputStream(Files.newInputStream(SAVE_PATH)))) {
      byte[] magic = in.readNBytes(4);
      if (!Arrays.equals(magic, MAGIC)) {
        throw new IllegalStateException("account.bin is corrupt or not a valid session file");
      }

      byte ver = in.readByte();
      if (ver != VERSION) {
        throw new IllegalStateException("Unsupported session format version: " + ver);
      }

      byte[] salt = in.readNBytes(SALT_LEN);
      in.readShort();
      int len = in.readInt();
      byte[] blob = in.readNBytes(len);

      SecretKey key = deriveKey(password, salt);
      Arrays.fill(password, '\0');

      byte[] plain = decrypt(key, blob);

      ByteBuffer buf = ByteBuffer.wrap(plain);
      int jsonLen = buf.getInt();
      byte[] jsonBytes = new byte[jsonLen];
      buf.get(jsonBytes);

      JsonObject json = JsonParser.parseString(new String(jsonBytes, StandardCharsets.UTF_8))
          .getAsJsonObject();
      return JavaAuthManager.fromJson(httpClient, json);
    }
  }

  public static JavaAuthManager login() throws Exception {
    HttpClient httpClient = MinecraftAuth.createHttpClient();
    Consumer<MsaDeviceCode> callback = code -> {
      System.out.println("Visit: " + code.getVerificationUri());
      System.out.println("Code:  " + code.getUserCode());
      System.out.println("Or: " + code.getDirectVerificationUri());
    };
    JavaAuthManager authManager = JavaAuthManager.create(httpClient)
        .login(DeviceCodeMsaAuthService::new, callback);
    saveSession(authManager);
    return authManager;
  }

  public static JavaAuthManager getSession() throws Exception {
    HttpClient httpClient = MinecraftAuth.createHttpClient();
    JavaAuthManager authManager = loadSession(httpClient);
    if (authManager == null) {
      return login();
    }
    authManager.getMinecraftProfile().refresh();
    saveSession(authManager);
    return authManager;
  }

  public static void logout() throws IOException {
    Files.deleteIfExists(SAVE_PATH);
  }

  public static boolean hasSession() {
    return Files.exists(SAVE_PATH);
  }
}