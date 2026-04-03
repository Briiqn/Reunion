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

package dev.briiqn.reunion.core.command.impl.console.debug;

import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.Command;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HeapCommand extends Command {

  public HeapCommand() {
    super("heap", "heap [top N]", "Shows heap usage");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    int top = 20;
    if (args.length >= 1) {
      try {
        top = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        log.info("Usage: " + getUsage());
        return;
      }
    }

    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      javax.management.ObjectName name = new javax.management.ObjectName(
          "com.sun.management:type=DiagnosticCommand");

      Object result = mbs.invoke(name, "gcClassHistogram",
          new Object[]{new String[0]},
          new String[]{String[].class.getName()});

      String histogram = result.toString();
      String[] lines = histogram.split("\n");

      int shown = 0;
      for (String line : lines) {
        if (line.isBlank()) {
          continue;
        }
        log.info(line);
        if (++shown >= top + 3) {
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}