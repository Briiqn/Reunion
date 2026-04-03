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

package dev.briiqn.reunion.api.packet;

/**
 * Describes the direction a packet is travelling.
 */
public enum PacketDirection {

  /**
   * Console client → Reunion proxy (inbound from the LCE client).
   */
  CONSOLE_TO_PROXY,

  /**
   * Reunion proxy → Java backend server (outbound to the backend).
   */
  PROXY_TO_JAVA,

  /**
   * Java backend server → Reunion proxy (inbound from the backend).
   */
  JAVA_TO_PROXY,

  /**
   * Reunion proxy → Console client (outbound to the LCE client).
   */
  PROXY_TO_CONSOLE
}
