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

package dev.briiqn.reunion.core.network.packet.protocol.java.s2c.impl;

import static dev.briiqn.reunion.core.util.StringUtil.readJavaString;
import static dev.briiqn.reunion.core.util.game.BufferUtil.posX;
import static dev.briiqn.reunion.core.util.game.BufferUtil.posY;
import static dev.briiqn.reunion.core.util.game.BufferUtil.posZ;

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAddPaintingS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import java.util.Set;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x10, supports = {47})
public final class JavaSpawnPaintingS2CPacket extends JavaS2CPacket {

  private static final Set<String> KNOWN_MOTIVES = Set.of(
      "Kebab", "Aztec", "Alban", "Aztec2", "Bomb", "Plant", "Wasteland",
      "Pool", "Courbet", "Sea", "Sunset", "Creebet",
      "Wanderer", "Graham",
      "Match", "Bust", "Stage", "Void", "SkullAndRoses", "Wither", "Fighters",
      "Pointer", "Pigscene", "BurningSkull",
      "Skeleton", "DonkeyKong"
  );

  private int entityId;
  private String title;
  private long pos;
  private int dir;

  public JavaSpawnPaintingS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    entityId = VarIntUtil.read(buf);
    title = readJavaString(buf);
    pos = buf.readLong();
    dir = buf.readUnsignedByte();
  }

  @Override
  public void handle(JavaSession session) {
    var cs = session.getConsoleSession();
    var em = cs.getEntityManager();

    int cid = em.map(entityId);
    String title = toLegacyMotive(this.title);

    em.registerCategory(entityId,
        dev.briiqn.reunion.core.manager.EntityManager.EntityCategory.PAINTING);
    em.setExtraData(entityId, new Object[]{title, dir});
    int bx = posX(pos);
    int by = posY(pos);
    int bz = posZ(pos);
    em.setPosition(entityId, bx * 32, by * 32, bz * 32);
    int cx = cs.toConsoleX(bx);
    int cz = cs.toConsoleZ(bz);

    PacketManager.sendToConsole(cs,
        new ConsoleAddPaintingS2CPacket(cid, title, cx, by, cz, dir));
  }

  private static String toLegacyMotive(String javaName) {
    String name = javaName.contains(":") ? javaName.substring(javaName.indexOf(':') + 1) : javaName;
    String lower = name.toLowerCase();
    String result = switch (lower) {
      case "skull_and_roses", "skullandroses" -> "SkullAndRoses";
      case "burning_skull",   "burningskull"  -> "BurningSkull";
      case "donkey_kong",     "donkeykong"    -> "DonkeyKong";
      default -> lower.isEmpty() ? "Kebab" : Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    };
    return KNOWN_MOTIVES.contains(result) ? result : "Kebab";
  }
}