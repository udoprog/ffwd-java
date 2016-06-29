/**
 * Copyright 2013-2014 Spotify AB. All rights reserved.
 *
 * The contents of this file are licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 **/
package com.spotify.ffwd.protobuf;

import com.spotify.ffwd.input.InputChannelInboundHandler;
import com.spotify.ffwd.input.InputPluginScope;
import com.spotify.ffwd.protocol.ProtocolServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import javax.inject.Inject;

/**
 * Decode a stream of data which is length-prefixed.
 * <p>
 * Should only be used with TCP-based protocols.
 *
 * @author udoprog
 */
@InputPluginScope
public class ProtobufLengthPrefixedProtocolServer implements ProtocolServer {
    private static final int MAX_LENGTH = 0xffffff;

    private final ChannelInboundHandler handler;
    private final ProtobufDecoder decoder;

    @Inject
    public ProtobufLengthPrefixedProtocolServer(
        final InputChannelInboundHandler handler, final ProtobufDecoder decoder
    ) {
        this.handler = handler;
        this.decoder = decoder;
    }

    @Override
    public final ChannelInitializer<Channel> initializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch
                    .pipeline()
                    .addLast(new LengthFieldBasedFrameDecoder(MAX_LENGTH, 0, 4), decoder, handler);
            }
        };
    }
}
