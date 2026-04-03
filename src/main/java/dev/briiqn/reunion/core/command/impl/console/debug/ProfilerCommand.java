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

import dev.briiqn.reunion.api.plugin.PluginManager;
import dev.briiqn.reunion.core.ReunionServer;
import dev.briiqn.reunion.core.command.Command;
import dev.briiqn.reunion.core.profiler.SparkProfiler;
import java.nio.file.Path;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ProfilerCommand extends Command {

  private SparkProfiler profiler;

  public ProfilerCommand() {
    super("profiler", "profiler <start [--alloc] [interval_ms]|stop [--upload]|status>",
        "CPU/alloc profiler  outputs .sparkprofile for spark.lucko.me");
  }

  @Override
  public void execute(ReunionServer server, String[] args) {
    if (args.length < 1) {
      log.info("Usage: " + getUsage());
      return;
    }
    switch (args[0].toLowerCase()) {
      case "start" -> start(args, server.getPluginApi().pluginManager());
      case "stop" -> stop(args);
      case "status" -> log.info("Profiler is {}",
          profiler != null && profiler.isRunning() ? "running" : "stopped");
      default -> log.info("Usage: " + getUsage());
    }
  }

  private void start(String[] args, PluginManager pluginManager) {
    if (profiler != null && profiler.isRunning()) {
      log.info("Profiler already running. Use 'profiler stop' first.");
      return;
    }

    boolean alloc = false;
    int interval = 10;
    for (int i = 1; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("--alloc")) {
        alloc = true;
      } else {
        try {
          interval = Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
          log.info("Invalid interval: {}", args[i]);
          return;
        }
      }
    }

    try {
      profiler = new SparkProfiler(interval, pluginManager);
      SparkProfiler.Mode mode = alloc ? SparkProfiler.Mode.ALLOC : SparkProfiler.Mode.CPU;
      profiler.start(mode);
      log.info("Profiler started. Run 'profiler stop [--upload]' when done.");
    } catch (Exception e) {
      log.error("Failed to start profiler: {}", e.getMessage());
    }
  }

  private void stop(String[] args) {
    if (profiler == null || !profiler.isRunning()) {
      log.info("Profiler is not running.");
      return;
    }

    boolean upload = args.length >= 2 && args[1].equalsIgnoreCase("--upload");

    if (upload) {
      Thread.ofVirtual().name("profiler-upload").start(() -> {
        try {
          String url = profiler.stopAndUpload();
          log.info("Profile uploaded: {}", url);
        } catch (Exception e) {
          log.error("Upload failed: {}", e.getMessage());
        }
      });
      log.info("Stopping profiler and uploading...");
    } else {
      try {
        Path out = profiler.stop();
        log.info("Profile saved to {}. Drag into spark.lucko.me to view.", out.toAbsolutePath());
      } catch (Exception e) {
        log.error("Failed to save profile: {}", e.getMessage());
      }
    }
  }
}