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

package dev.briiqn.reunion.core.network.packet.protocol.console.c2s.impl;

import dev.briiqn.reunion.api.world.Dimension;
import dev.briiqn.reunion.api.world.Location;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.c2s.ConsoleC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaPlayerC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaPlayerLookC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaPlayerPositionC2SPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaPlayerPositionLookC2SPacket;
import dev.briiqn.reunion.core.plugin.hooks.PluginEventHooks;
import dev.briiqn.reunion.core.plugin.session.ConsoleSessionPlayerAdapter;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.util.math.vector.Vec2f;
import dev.briiqn.reunion.core.util.math.vector.Vec3d;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

public abstract class ConsolePlayerFlyingC2SPacket extends ConsoleC2SPacket {

  @Getter
  protected Vec3d pos = Vec3d.ZERO;
  @Getter
  protected double stance = 0.0;
  @Getter
  protected Vec2f rot = Vec2f.ZERO;
  @Getter
  protected boolean onGround;
  @Getter
  protected boolean hasPos, hasRot;

  public double getX() {
    return pos.x();
  }

  public double getY() {
    return pos.y();
  }

  public double getZ() {
    return pos.z();
  }

  public float getYaw() {
    return rot.yaw();
  }

  public float getPitch() {
    return rot.pitch();
  }

  @Override
  public void write(ByteBuf buf) {
  }

  @Override
  public void handle(ConsoleSession session) {

    if (session.isWaitingForInitialTeleport()) {
      return;
    }

    if (hasPos) {
      double px = pos.x(), pz = pos.z();
      if (Math.abs(px) > 2.9999999E7D || Math.abs(pz) > 2.9999999E7D) {
        return;
      }
      session.markClientScreenReady();
    }

    final Vec3d lastPos = session.getLastPos();
    final Vec2f lastRot = session.getLastRot();

    double javaX = hasPos ? session.toJavaX(pos.x()) : lastPos.x();
    double javaY = hasPos ? pos.y() : lastPos.y();
    double javaZ = hasPos ? session.toJavaZ(pos.z()) : lastPos.z();
    float newYaw = hasRot ? rot.yaw() : lastRot.yaw();
    float newPitch = hasRot ? rot.pitch() : lastRot.pitch();

    if (session.hasPendingTeleport()) {
      if (hasPos && hasRot) {
        Vec3d coords = session.consumePendingTeleport();
        Vec2f teleportRot = lastRot;
        session.setLastPos(new Vec3d(javaX, javaY, javaZ));
        session.setLastRot(new Vec2f(newYaw, newPitch));
        session.getPendingTeleportAcks().incrementAndGet();
        PacketManager.sendToJava(session.getJavaSession(),
            new JavaPlayerPositionLookC2SPacket(
                coords.x(), coords.y(), coords.z(),
                teleportRot.yaw(), teleportRot.pitch(),
                false));
      } else {
        PacketManager.sendToJava(session.getJavaSession(), new JavaPlayerC2SPacket(onGround));
      }
      return;
    }

    if (session.getPendingTeleportAcks().get() > 0) {
      if (hasPos || hasRot) {
        session.getPendingTeleportAcks().decrementAndGet();
      }
      PacketManager.sendToJava(session.getJavaSession(), new JavaPlayerC2SPacket(onGround));
      return;
    }

    if (hasPos || hasRot) {
      Location from = Location.of(
          lastPos.x(), lastPos.y(), lastPos.z(),
          lastRot.yaw(), lastRot.pitch(),
          Dimension.fromIdOrDefault(session.getDimension()));
      Location to = Location.of(javaX, javaY, javaZ, newYaw, newPitch,
          Dimension.fromIdOrDefault(session.getDimension()));

      if (!PluginEventHooks.fireMove(new ConsoleSessionPlayerAdapter(session), from, to)) {
        return;
      }
    }

    if (hasPos) {
      session.checkWorldBounds(javaX, javaZ);
      session.setLastPos(new Vec3d(javaX, javaY, javaZ));
    }

    boolean rotationChanged = false;
    if (hasRot) {
      Vec2f newRot = new Vec2f(newYaw, newPitch);
      rotationChanged = newRot.hasChanged(lastRot);
      session.setLastRot(newRot);
    }

    if (hasPos || (!hasPos && !hasRot)) {
      session.flushTickActions();
    }

    if (hasPos && hasRot) {
      if (rotationChanged) {
        PacketManager.sendToJava(session.getJavaSession(),
            new JavaPlayerPositionLookC2SPacket(javaX, javaY, javaZ, newYaw, newPitch, onGround));
      } else {
        PacketManager.sendToJava(session.getJavaSession(),
            new JavaPlayerPositionC2SPacket(javaX, javaY, javaZ, onGround));
      }
    } else if (hasPos) {
      PacketManager.sendToJava(session.getJavaSession(),
          new JavaPlayerPositionC2SPacket(javaX, javaY, javaZ, onGround));
    } else if (hasRot) {
      if (rotationChanged) {
        PacketManager.sendToJava(session.getJavaSession(),
            new JavaPlayerLookC2SPacket(newYaw, newPitch, onGround));
      } else {
        PacketManager.sendToJava(session.getJavaSession(), new JavaPlayerC2SPacket(onGround));
      }
    } else {
      PacketManager.sendToJava(session.getJavaSession(), new JavaPlayerC2SPacket(onGround));
    }
  }
}