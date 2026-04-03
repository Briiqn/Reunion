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

package dev.briiqn.reunion.core.network.pipeline.console.handler;

import dev.briiqn.reunion.core.manager.EntityManager;
import dev.briiqn.reunion.core.manager.PlayerManager;
import dev.briiqn.reunion.core.network.packet.data.enums.TextureChangeType;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddEntityS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddExperienceOrbS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddGlobalEntityS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddMobS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddPaintingS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddPlayerS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsolePlayerInfoS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleRemoveEntitiesS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleTextureChangeS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.impl.JavaSpawnPlayerS2CPacket;
import dev.briiqn.reunion.core.registry.impl.EntityRegistry;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ConsoleWorldHackHandler {

  private static final int THRESHOLD_OVERWORLD = 1500;
  private static final int THRESHOLD_OTHER = 256;

  private final ConsoleSession session;

  public ConsoleWorldHackHandler(ConsoleSession session) {
    this.session = session;
  }

  public double toConsoleX(double javaX) {
    return isEnabled() ? javaX - session.getWorldOffsetX() : javaX;
  }

  public double toConsoleZ(double javaZ) {
    return isEnabled() ? javaZ - session.getWorldOffsetZ() : javaZ;
  }

  public double toJavaX(double consoleX) {
    if (!isEnabled()) {
      return consoleX;
    }
    double t = consoleX + session.getWorldOffsetX();
    return Math.abs(t) > 30_000_000 ? session.getLastX() : t;
  }

  public double toJavaZ(double consoleZ) {
    if (!isEnabled()) {
      return consoleZ;
    }
    double t = consoleZ + session.getWorldOffsetZ();
    return Math.abs(t) > 30_000_000 ? session.getLastZ() : t;
  }

  public int toConsoleX(int javaX) {
    return isEnabled() ? javaX - session.getWorldOffsetX() : javaX;
  }

  public int toConsoleZ(int javaZ) {
    return isEnabled() ? javaZ - session.getWorldOffsetZ() : javaZ;
  }

  public int toJavaX(int consoleX) {
    return isEnabled() ? consoleX + session.getWorldOffsetX() : consoleX;
  }

  public int toJavaZ(int consoleZ) {
    return isEnabled() ? consoleZ + session.getWorldOffsetZ() : consoleZ;
  }

  public int getThreshold() {
    int dim = session.getDimension();
    return (dim == -1 || dim == 1) ? THRESHOLD_OTHER : THRESHOLD_OVERWORLD;
  }

  public void checkWorldBounds(double javaX, double javaZ) {
    if (!isEnabled()) {
      return;
    }

    double consoleX = javaX - session.getWorldOffsetX();
    double consoleZ = javaZ - session.getWorldOffsetZ();
    int threshold = getThreshold();

    if (Math.abs(consoleX) <= threshold && Math.abs(consoleZ) <= threshold) {
      return;
    }

    removeAllNonPlayerEntities();
    unloadAllChunks();

    session.setWorldOffsetX((((int) javaX) >> 4) * 16);
    session.setWorldOffsetZ((((int) javaZ) >> 4) * 16);

    resendAllChunks();
    respawnAllTrackedEntities();
  }

  private boolean isEnabled() {
    return session.getServer().getConfig().getGameplay().isInfiniteWorldHack();
  }

  private void removeAllNonPlayerEntities() {
    EntityManager em = session.getEntityManager();
    List<Integer> toRemove = new ArrayList<>();
    for (int cid : em.getConsoleIds()) {
      if (cid != session.getSafePlayerId()) {
        toRemove.add(cid);
      }
    }
    sendRemoveEntities(toRemove);
  }

  private void sendRemoveEntities(List<Integer> ids) {
    if (ids.isEmpty()) {
      return;
    }
    for (int i = 0; i < ids.size(); i += 255) {
      int end = Math.min(i + 255, ids.size());
      PacketManager.sendToConsole(session,
          new ConsoleRemoveEntitiesS2CPacket(ids.subList(i, end)));
    }
  }

  private void unloadAllChunks() {
    if (session.getChunkManager() != null) {
      session.getChunkManager().unloadAllChunks();
    }
  }

  private void resendAllChunks() {
    if (session.getChunkManager() != null) {
      session.getChunkManager().resendAllChunks();
    }
  }

  private void respawnAllTrackedEntities() {
    EntityManager em = session.getEntityManager();
    int worldOX = session.getWorldOffsetX();
    int worldOZ = session.getWorldOffsetZ();

    for (int javaId : em.getJavaIds()) {
      if (javaId == session.getJavaEntityId()) {
        continue;
      }

      int cid = em.getConsoleId(javaId);
      EntityManager.EntityCategory category = em.getCategory(javaId);
      if (category == null) {
        continue;
      }

      int localX = em.getX(javaId) - (worldOX * 32);
      int localZ = em.getZ(javaId) - (worldOZ * 32);
      int localY = em.getY(javaId);
      byte yaw = em.getYaw(javaId);
      byte pitch = em.getPitch(javaId);

      switch (category) {
        case MOB -> spawnMob(javaId, cid, localX, localY, localZ, yaw, pitch);
        case OBJECT -> spawnObject(javaId, cid, localX, localY, localZ, yaw, pitch);
        case PLAYER -> spawnPlayer(javaId, cid, localX, localY, localZ, yaw, pitch);
        case PAINTING -> spawnPainting(javaId, cid, localX, localY, localZ);
        case EXPERIENCE_ORB -> spawnExperienceOrb(javaId, cid, localX, localY, localZ);
        case GLOBAL_ENTITY -> spawnGlobalEntity(javaId, cid, localX, localY, localZ);
      }
    }
  }

  private void spawnMob(int javaId, int cid,
      int lx, int ly, int lz, byte yaw, byte pitch) {
    Integer type = session.getEntityManager().getType(javaId);
    if (type == null) {
      return;
    }

    int consoleType = type;
    EntityRegistry registry = EntityRegistry.getInstance();
    if (type == 30) {
      consoleType = 54;
    } else if (registry.get(type) == null || registry.isUnsupported(type)) {
      consoleType = registry.remap(type);
      if (registry.get(consoleType) == null) {
        consoleType = 54;
      }
    }

    PacketManager.sendToConsole(session, new ConsoleAddMobS2CPacket(
        cid, consoleType, lx, ly, lz, yaw, pitch, yaw,
        (short) 0, (short) 0, (short) 0, (byte) 0));
  }

  private void spawnObject(int javaId, int cid,
      int lx, int ly, int lz, byte yaw, byte pitch) {
    Integer type = session.getEntityManager().getType(javaId);
    if (type == null) {
      return;
    }

    int objectData = session.getEntityManager().getObjectData(javaId);
    if (type == dev.briiqn.reunion.core.network.packet.data.enums.EntityType.ARMOR_STAND.getId()) {
      PacketManager.sendToConsole(session, new ConsoleAddMobS2CPacket(
          cid, 54, lx, ly, lz, yaw, pitch, yaw,
          (short) 0, (short) 0, (short) 0, (byte) 0));
    } else {
      PacketManager.sendToConsole(session, new ConsoleAddEntityS2CPacket(
          cid, type, lx, ly, lz, yaw, pitch, objectData,
          (short) 0, (short) 0, (short) 0));
    }
  }

  private void spawnPlayer(int javaId, int cid,
      int lx, int ly, int lz, byte yaw, byte pitch) {
    Object[] playerData = (Object[]) session.getEntityManager().getExtraData(javaId);
    if (playerData == null || !(playerData[0] instanceof UUID pUuid)) {
      return;
    }

    PlayerManager pm = session.getPlayerManager();
    String name = pm.getName(pUuid);
    long xuid = pUuid.getMostSignificantBits();
    long onlineXuid = pUuid.getLeastSignificantBits();
    short heldItem = (playerData.length > 1 && playerData[1] instanceof Short s) ? s : 0;
    byte eFlags = (playerData.length > 2 && playerData[2] instanceof Byte b) ? b : 0;

    String skinName = pm.getSkin(pUuid);
    String capeName = pm.getCape(pUuid);

    ConsoleSession peer = findPeerByUuid(pUuid);
    int lceSkinId = (skinName == null && peer != null) ? peer.getLceSkinDwId() : 0;

    PacketManager.sendToConsole(session, new ConsoleAddPlayerS2CPacket(
        cid, name, lx, ly, lz, yaw, pitch, yaw, heldItem, eFlags,
        xuid, onlineXuid, lceSkinId, 0));

    if (skinName != null) {
      PacketManager.sendToConsole(session,
          new ConsoleTextureChangeS2CPacket(cid, TextureChangeType.SKIN, skinName));
    }
    if (capeName != null) {
      PacketManager.sendToConsole(session,
          new ConsoleTextureChangeS2CPacket(cid, TextureChangeType.CAPE, capeName));
    }

    if (peer != null) {
      if (skinName == null && peer.getLceSkinName() != null) {
        JavaSpawnPlayerS2CPacket.sendLceSkin(
            session, cid, peer.getLceSkinName(), peer.getLceSkinDwId(), peer);
      }
      if (capeName == null && peer.getLceCapeName() != null) {
        JavaSpawnPlayerS2CPacket.sendLceTexture(
            session, cid, peer.getLceCapeName(), 0, peer, TextureChangeType.CAPE);
      }
    }

    Byte rsmid = pm.getSmallId(pUuid);
    Integer cidx = pm.getColorIdx(pUuid);
    if (rsmid != null && cidx != null) {
      PacketManager.sendToConsole(session, new ConsolePlayerInfoS2CPacket(
          rsmid, cidx.shortValue(), session.getPlayerPrivileges(), cid));
    }
  }

  private void spawnPainting(int javaId, int cid, int lx, int ly, int lz) {
    Object[] data = (Object[]) session.getEntityManager().getExtraData(javaId);
    if (data != null && data.length == 2) {
      PacketManager.sendToConsole(session, new ConsoleAddPaintingS2CPacket(
          cid, (String) data[0], lx, ly, lz, (int) data[1]));
    }
  }

  private void spawnExperienceOrb(int javaId, int cid, int lx, int ly, int lz) {
    Short count = (Short) session.getEntityManager().getExtraData(javaId);
    if (count != null) {
      PacketManager.sendToConsole(session,
          new ConsoleAddExperienceOrbS2CPacket(cid, lx, ly, lz, count));
    }
  }

  private void spawnGlobalEntity(int javaId, int cid, int lx, int ly, int lz) {
    Byte type = (Byte) session.getEntityManager().getExtraData(javaId);
    if (type != null) {
      PacketManager.sendToConsole(session,
          new ConsoleAddGlobalEntityS2CPacket(cid, type, lx, ly, lz));
    }
  }

  private ConsoleSession findPeerByUuid(UUID pUuid) {
    for (ConsoleSession s : session.getServer().getSessions().values()) {
      UUID su = s.getUuid();
      if (su != null && su.equals(pUuid)) {
        return s;
      }
    }
    return null;
  }
}