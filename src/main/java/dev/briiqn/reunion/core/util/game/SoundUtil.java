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

import java.util.HashMap;
import java.util.Map;

public final class SoundUtil {

  private static final String[] CONSOLE_SOUNDS_LCE = {
      "mob.chicken", "mob.chickenhurt", "mob.chickenplop", "mob.cow", "mob.cowhurt", // 0-4
      "mob.pig", "mob.pigdeath", "mob.sheep", "mob.wolf.growl", "mob.wolf.whine", // 5-9
      "mob.wolf.panting", "mob.wolf.bark", "mob.wolf.hurt", "mob.wolf.death", "mob.wolf.shake",
      // 10-14
      "mob.blaze.breathe", "mob.blaze.hit", "mob.blaze.death", "mob.ghast.moan", "mob.ghast.scream",
      // 15-19
      "mob.ghast.death", "mob.ghast.fireball", "mob.ghast.charge", // 20-22
      "mob.endermen.idle", "mob.endermen.hit", "mob.endermen.death", "mob.endermen.portal", // 23-26
      "mob.zombiepig.zpig", "mob.zombiepig.zpighurt", "mob.zombiepig.zpigdeath",
      "mob.zombiepig.zpigangry", // 27-30
      "mob.silverfish.say", "mob.silverfish.hit", "mob.silverfish.kill", "mob.silverfish.step",
      // 31-34
      "mob.skeleton", "mob.skeletonhurt", "mob.spider", "mob.spiderdeath", // 35-38
      "mob.slime", "mob.slimeattack", "mob.creeper", "mob.creeperdeath", // 39-42
      "mob.zombie", "mob.zombiehurt", "mob.zombiedeath", "mob.zombie.wood", // 43-46
      "mob.zombie.woodbreak", "mob.zombie.metal", "mob.magmacube.big", "mob.magmacube.small",
      // 47-50
      "mob.cat.purr", "mob.cat.purreow", "mob.cat.meow", "mob.cat.hit", // 51-54
      "random.bow", "random.bowhit", "random.explode", "random.fizz", "random.pop", // 55-59
      "random.fuse", "random.drink", "random.eat", "random.burp", "random.splash", // 60-64
      "random.click", "random.glass", "random.orb", "random.break", "random.chestopen", // 65-69
      "random.chestclosed", "random.door_open", "random.door_close", // 70-72
      "ambient.weather.rain", "ambient.weather.thunder", "ambient.cave.cave",
      // 73-75 (cave2 skipped on PC)
      "portal.portal", "portal.trigger", "portal.travel", // 76-78
      "fire.ignite", "fire.fire", // 79-80
      "damage.hurtflesh", "damage.fallsmall", "damage.fallbig", // 81-83
      "note.harp", "note.bd", "note.snare", "note.hat", "note.bassattack", // 84-88
      "tile.piston.in", "tile.piston.out", "liquid.water", "liquid.lavapop", "liquid.lava", // 89-93
      "step.stone", "step.wood", "step.gravel", "step.grass", "step.metal", "step.cloth",
      "step.sand", // 94-100
      "mob.enderdragon.end", "mob.enderdragon.growl", "mob.enderdragon.hit",
      "mob.enderdragon.wings", // 101-104
      "mob.irongolem.throw", "mob.irongolem.hit", "mob.irongolem.death", "mob.irongolem.walk",
      // 105-108
      "damage.thorns", "random.anvil_break", "random.anvil_land", "random.anvil_use", // 109-112
      "mob.villager.haggle", "mob.villager.idle", "mob.villager.hit", "mob.villager.death",
      // 113-116
      "mob.villager.yes", "mob.villager.no", "mob.zombie.infect", "mob.zombie.unfect", // 117-120
      "mob.zombie.remedy", "step.snow", "step.ladder", // 121-123
      "dig.cloth", "dig.grass", "dig.gravel", "dig.sand", "dig.snow", "dig.stone", "dig.wood"
      // 124-130
  };

  private static final Map<String, Integer> SOUND_MAP = new HashMap<>();

  static {
    for (int i = 0; i < CONSOLE_SOUNDS_LCE.length; i++) {
      SOUND_MAP.put(CONSOLE_SOUNDS_LCE[i], i);
    }
  }

  public static int getConsoleSoundId(String javaName) {
    if (javaName == null) {
      return 64;
    }

    String name = javaName.startsWith("minecraft:") ? javaName.substring(10) : javaName;

    if (name.contains(".primed") || name.contains(".fuse")) {
      return 60;
    }
    if (name.contains("fall.big")) {
      return 83;
    }
    if (name.contains("fall.small")) {
      return 82;
    }
    if (name.contains(".hurt") || name.contains(".attack") || name.contains("damage.hit")) {
      if (name.contains("zombie")) {
        return 44;
      }
      if (name.contains("skeleton")) {
        return 36;
      }
      if (name.contains("blaze")) {
        return 16;
      }
      if (name.contains("creeper")) {
        return 41;
      }
      return 81;
    }

    if (name.contains("block.") || name.contains(".place") || name.contains(".break")
        || name.contains("step.") || name.contains(".step")) {
      if (name.contains("grass") || name.contains("leaves") || name.contains("dirt")) {
        return 125;
      }
      if (name.contains("wood") || name.contains("ladder") || name.contains("fence")) {
        return 130;
      }
      if (name.contains("gravel")) {
        return 126;
      }
      if (name.contains("sand")) {
        return 127;
      }
      if (name.contains("snow")) {
        return 128;
      }
      if (name.contains("cloth") || name.contains("wool")) {
        return 124;
      }
      return 129;
    }

    if (SOUND_MAP.containsKey(name)) {
      return SOUND_MAP.get(name);
    }

    for (Map.Entry<String, Integer> entry : SOUND_MAP.entrySet()) {
      if (name.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    return -1;
  }
}