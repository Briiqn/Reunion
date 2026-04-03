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

package dev.briiqn.reunion.api.command;

import java.util.Collections;
import java.util.List;

/**
 * A simple proxy-side command.
 *
 * <p>Implement this interface to register a command that can be run from
 * the proxy console or by players (if forwarded by your plugin).
 *
 * <p>Example:
 * <pre>{@code
 * public class AlertCommand implements SimpleCommand {
 *
 *     private final ReunionProxy proxy;
 *
 *     public AlertCommand(ReunionProxy proxy) { this.proxy = proxy; }
 *
 *     @Override
 *     public void execute(Invocation invocation) {
 *         if (invocation.arguments().length == 0) {
 *             invocation.source().sendMessage("Usage: /alert <message>");
 *             return;
 *         }
 *         String msg = String.join(" ", invocation.arguments());
 *         proxy.allPlayers().forEach(p -> p.sendMessage("[Alert] " + msg));
 *     }
 *
 *     @Override
 *     public List<String> suggest(Invocation invocation) {
 *         return Collections.emptyList();
 *     }
 * }
 * }</pre>
 *
 * <p>Register with:
 * <pre>{@code
 * proxy().commandManager().register(CommandMeta.of("alert"), new AlertCommand(proxy()));
 * }</pre>
 */
public interface SimpleCommand {

  /**
   * Called when the command is executed.
   *
   * @param invocation the invocation context
   */
  void execute(Invocation invocation);

  /**
   * UNIMPLEMENTED Returns suggestions for the current input.
   *
   * @param invocation the invocation context with partial arguments
   * @return a list of completion suggestions; may be empty, never null
   */
  default List<String> suggest(Invocation invocation) {
    return Collections.emptyList();
  }

  /**
   * Returns {@code true} if the given source is permitted to run this command.
   *
   * <p>The default implementation allows everyone. Override to restrict access.
   *
   * @param source the command source
   * @return {@code true} if allowed
   */
  default boolean hasPermission(CommandSource source) {
    return true;
  }


  /**
   * Encapsulates the context of a single command invocation.
   */
  interface Invocation {

    /**
     * The source that executed the command.
     */
    CommandSource source();

    /**
     * The alias that was used to invoke this command. For the command registered as "alert", this
     * will be "alert".
     */
    String alias();

    /**
     * The arguments passed after the command name, split by spaces. Returns an empty array if no
     * arguments were provided.
     */
    String[] arguments();

    /**
     * The full raw argument string, not split. Returns an empty string if no arguments were
     * provided.
     */
    String rawArguments();
  }
}
