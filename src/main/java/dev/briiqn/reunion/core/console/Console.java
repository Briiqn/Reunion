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

package dev.briiqn.reunion.core.console;

import dev.briiqn.reunion.core.ReunionServer;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

@AllArgsConstructor
@Log4j2
public class Console {

  ReunionServer server;

  public void start() {
    Terminal terminal;
    try {
      terminal = TerminalBuilder.builder().system(true).build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .option(LineReader.Option.AUTO_FRESH_LINE, true)
        .build();

    installJLineAppender(reader);

    log.info("Type 'help' for commands.");

    while (true) {
      String line;
      try {
        line = reader.readLine("> ");
      } catch (Exception e) {
        break;
      }

      if (line == null || line.trim().isEmpty()) {
        continue;
      }

      server.getCommandManager().dispatch(line);
    }
  }

  private void installJLineAppender(LineReader reader) {
    org.apache.logging.log4j.status.StatusLogger.getLogger()
        .setLevel(org.apache.logging.log4j.Level.WARN);

    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    var config = context.getConfiguration();

    PatternLayout layout = PatternLayout.newBuilder()
        .withPattern("%d{HH:mm:ss} [%level] %msg%n")
        .build();

    AbstractAppender jlineAppender = new AbstractAppender(
        "JLineAppender", null, layout, true, Property.EMPTY_ARRAY
    ) {
      @Override
      public void append(LogEvent event) {
        String message = new String(layout.toByteArray(event));
        reader.printAbove(message.stripTrailing());
      }
    };
    jlineAppender.start();

    var rootLogger = config.getRootLogger();
    rootLogger.getAppenders().keySet().forEach(rootLogger::removeAppender);

    rootLogger.addAppender(jlineAppender, null, null);
    context.updateLoggers();
  }
}