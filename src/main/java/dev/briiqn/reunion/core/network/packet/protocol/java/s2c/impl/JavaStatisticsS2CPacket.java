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

import dev.briiqn.reunion.core.network.packet.annotation.PacketInfo;
import dev.briiqn.reunion.core.network.packet.data.PacketSide;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleAwardStatS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.s2c.JavaS2CPacket;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

@PacketInfo(side = PacketSide.JAVA_S2C, id = 0x37, supports = {47})
public final class JavaStatisticsS2CPacket extends JavaS2CPacket {

  // const int Achievements::ACHIEVEMENT_OFFSET = 0x500000;
  private static final int ACHIEVEMENT_OFFSET = 5242880;
  private final Map<String, Integer> statistics = new HashMap<>();

  public JavaStatisticsS2CPacket() {
  }

  @Override
  public void read(ByteBuf buf) {
    int count = VarIntUtil.read(buf);
    for (int i = 0; i < count; i++) {
      String statName = readJavaString(buf);
      int value = VarIntUtil.read(buf);
      statistics.put(statName, value);
    }
  }

  @Override
  public void handle(JavaSession session) {
    for (Map.Entry<String, Integer> entry : statistics.entrySet()) {
      String statName = entry.getKey();
      int value = entry.getValue();

      if (value > 0 && statName.startsWith("achievement.")) {
        int consoleStatId = mapAchievementToId(statName);
        if (consoleStatId != -1) {
          PacketManager.sendToConsole(session.getConsoleSession(),
              new ConsoleAwardStatS2CPacket(consoleStatId));
        }
      }
    }
  }

  private int mapAchievementToId(String name) {
    return switch (name) {
      case "achievement.openInventory" -> ACHIEVEMENT_OFFSET;
      case "achievement.mineWood" -> ACHIEVEMENT_OFFSET + 1;
      case "achievement.buildWorkBench" -> ACHIEVEMENT_OFFSET + 2;
      case "achievement.buildPickaxe" -> ACHIEVEMENT_OFFSET + 3;
      case "achievement.buildFurnace" -> ACHIEVEMENT_OFFSET + 4;
      case "achievement.acquireIron" -> ACHIEVEMENT_OFFSET + 5;
      case "achievement.buildHoe" -> ACHIEVEMENT_OFFSET + 6;
      case "achievement.makeBread" -> ACHIEVEMENT_OFFSET + 7;
      case "achievement.bakeCake" -> ACHIEVEMENT_OFFSET + 8;
      case "achievement.buildBetterPickaxe" -> ACHIEVEMENT_OFFSET + 9;
      case "achievement.cookFish" -> ACHIEVEMENT_OFFSET + 10;
      case "achievement.onARail" -> ACHIEVEMENT_OFFSET + 11;
      case "achievement.buildSword" -> ACHIEVEMENT_OFFSET + 12;
      case "achievement.killEnemy" -> ACHIEVEMENT_OFFSET + 13;
      case "achievement.killCow" -> ACHIEVEMENT_OFFSET + 14;
      case "achievement.flyPig" -> ACHIEVEMENT_OFFSET + 15;

      // Indices 16-18 are 4J exclusives (Leader, MOAR, Dispense)

      //  "InToTheNether" maps to "portal"
      case "achievement.portal" -> ACHIEVEMENT_OFFSET + 19;

      // 20-25 are also 4J exclusives (Mine100, Kill10Creepers, EatPork, etc.)

      case "achievement.snipeSkeleton" -> ACHIEVEMENT_OFFSET + 26;
      case "achievement.diamonds" -> ACHIEVEMENT_OFFSET + 27;
      case "achievement.ghast" -> ACHIEVEMENT_OFFSET + 28;
      case "achievement.blazeRod" -> ACHIEVEMENT_OFFSET + 29;
      case "achievement.potion" -> ACHIEVEMENT_OFFSET + 30;
      case "achievement.theEnd" -> ACHIEVEMENT_OFFSET + 31;
      case "achievement.theEnd2" -> ACHIEVEMENT_OFFSET + 32; // "winGame"
      case "achievement.enchantments" -> ACHIEVEMENT_OFFSET + 33;
      case "achievement.overkill" -> ACHIEVEMENT_OFFSET + 34;
      case "achievement.bookcase" -> ACHIEVEMENT_OFFSET + 35;

      case "achievement.exploreAllBiomes" -> ACHIEVEMENT_OFFSET + 36; // "adventuringTime"
      case "achievement.breedCow" -> ACHIEVEMENT_OFFSET + 37; // "repopulation"
      case "achievement.diamondsToYou" -> ACHIEVEMENT_OFFSET + 38;

      case "achievement.spawnWither" -> -1;
      case "achievement.killWither" -> -1;
      case "achievement.fullBeacon" -> -1;
      case "achievement.overpowered" -> -1;

      default -> -1;
    };
  }
}