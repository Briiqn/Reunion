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

package dev.briiqn.reunion.core.network.server;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.platform.ViaCodecHandler;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import dev.briiqn.reunion.core.network.pipeline.java.decode.JavaPacketDecoder;
import dev.briiqn.reunion.core.network.pipeline.java.decode.VarIntFrameDecoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaLengthPrefixEncoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaPacketEncoder;
import dev.briiqn.reunion.core.network.pipeline.java.encode.JavaRawPacketSerializer;
import dev.briiqn.reunion.core.session.ConsoleSession;
import dev.briiqn.reunion.core.session.JavaSession;
import dev.briiqn.reunion.core.via.ViaManager;
import io.netty.channel.socket.SocketChannel;


public final class ServerConnector {

  private ServerConnector() {
  }

  public static void initJavaPipeline(SocketChannel ch, ConsoleSession owner) {
    JavaSession session = new JavaSession(owner, owner.getServer());

    if (ViaManager.isEnabled()) {
      UserConnection conn = new UserConnectionImpl(ch, true);
      new ProtocolPipelineImpl(conn);

      ch.pipeline()
          .addLast("frame", new VarIntFrameDecoder())
          .addLast("length-encoder", new JavaLengthPrefixEncoder())
          .addLast("via-codec", new ViaCodecHandler(conn))
          .addLast("decoder", new JavaPacketDecoder())
          .addLast("encoder", new JavaRawPacketSerializer())
          .addLast("handler", session.javaChannelHandler());
    } else {
      ch.pipeline()
          .addLast("frame", new VarIntFrameDecoder())
          .addLast("decoder", new JavaPacketDecoder())
          .addLast("encoder", new JavaPacketEncoder())
          .addLast("handler", session.javaChannelHandler());
    }
  }
}