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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Metadata for registering a command with the {@link CommandManager}.
 *
 * <p>A command must have at least one name (its primary alias). Additional
 * aliases can be added and all will invoke the same handler.
 *
 * <p>Example:
 * <pre>{@code
 * CommandMeta meta = CommandMeta.builder("glist")
 *         .aliases("globallist", "players")
 *         .description("Lists all online players across all servers.")
 *         .build();
 * }</pre>
 *
 * <p>For quick one-liner registration without aliases or description:
 * <pre>{@code
 * CommandMeta.of("alert")
 * }</pre>
 */
public final class CommandMeta {

  private final String name;
  private final Set<String> aliases;
  private final String description;
  private final String usage;

  private CommandMeta(Builder builder) {
    this.name = builder.name.toLowerCase();
    this.aliases = Collections.unmodifiableSet(builder.aliases);
    this.description = builder.description;
    this.usage = builder.usage;
  }

  /**
   * Creates a {@link CommandMeta} with only a primary name and no aliases.
   */
  public static CommandMeta of(String name) {
    return builder(name).build();
  }

  /**
   * Creates a new {@link Builder} for the given primary command name.
   */
  public static Builder builder(String name) {
    return new Builder(name);
  }

  /**
   * The primary name of this command (lowercase).
   */
  public String name() {
    return name;
  }

  /**
   * All aliases this command responds to (including the primary name). The first element is always
   * the primary name.
   */
  public Set<String> aliases() {
    return aliases;
  }

  /**
   * A short description of what the command does.
   */
  public String description() {
    return description;
  }

  /**
   * Usage hint displayed when the command is used incorrectly.
   */
  public String usage() {
    return usage;
  }


  public static final class Builder {

    private final String name;
    private final Set<String> aliases = new LinkedHashSet<>();
    private String description = "";
    private String usage = "";

    private Builder(String name) {
      this.name = name;
      this.aliases.add(name.toLowerCase());
    }

    /**
     * Adds one or more aliases for this command.
     */
    public Builder aliases(String... aliases) {
      for (String alias : aliases) {
        this.aliases.add(alias.toLowerCase());
      }
      return this;
    }

    /**
     * Sets the command description.
     */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    /**
     * Sets the usage hint.
     */
    public Builder usage(String usage) {
      this.usage = usage;
      return this;
    }

    public CommandMeta build() {
      return new CommandMeta(this);
    }
  }
}
