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

package dev.briiqn.reunion.api.event;

import java.util.Optional;

/**
 * An event that carries an allow / deny / hold result.
 *
 * <p>Used for login-phase events where you need to deny access with a message,
 * or hold a player in a pending state (e.g. limbo) before proceeding.
 */
public abstract class ResultedEvent implements Event {

  private Result result = Result.allowed();

  /**
   * Returns the current result of this event.
   */
  public Result result() {
    return result;
  }

  /**
   * Sets the result to allowed  the normal flow continues.
   */
  public void allow() {
    this.result = Result.allowed();
  }

  /**
   * Denies the event and disconnects the player with the given reason.
   *
   * @param reason the message shown to the player
   */
  public void deny(String reason) {
    this.result = Result.denied(reason);
  }

  /**
   * Holds the player  keeps the console connection open but does NOT initiate a Java backend
   * connection. The plugin is responsible for sending a fake world and calling
   * {@code player.connectTo()} when ready.
   *
   * <p>Only meaningful on {@link dev.briiqn.reunion.api.event.player.PreLoginEvent}.
   */
  public void hold() {
    this.result = Result.held();
  }

  /**
   * Returns {@code true} if the result is allowed or held (not denied).
   */
  public boolean isAllowed() {
    return result.status != Result.Status.DENIED;
  }

  /**
   * Returns {@code true} if the player is being held (limbo).
   */
  public boolean isHeld() {
    return result.status == Result.Status.HELD;
  }


  public static final class Result {

    private final Status status;
    private final String reason;

    private Result(Status status, String reason) {
      this.status = status;
      this.reason = reason;
    }

    public static Result allowed() {
      return new Result(Status.ALLOWED, null);
    }

    public static Result denied(String reason) {
      return new Result(Status.DENIED, reason);
    }

    public static Result held() {
      return new Result(Status.HELD, null);
    }

    public Status status() {
      return status;
    }

    public boolean isAllowed() {
      return status == Status.ALLOWED;
    }

    public boolean isDenied() {
      return status == Status.DENIED;
    }

    public boolean isHeld() {
      return status == Status.HELD;
    }

    public Optional<String> reason() {
      return Optional.ofNullable(reason);
    }

    @Override
    public String toString() {
      return "Result{" + status + (reason != null ? ", reason=" + reason : "") + "}";
    }

    public enum Status {ALLOWED, DENIED, HELD}
  }
}