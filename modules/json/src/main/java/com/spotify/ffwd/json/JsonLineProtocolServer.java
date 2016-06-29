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
package com.spotify.ffwd.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.ffwd.input.InputChannelInboundHandler;
import com.spotify.ffwd.input.InputPluginScope;
import com.spotify.ffwd.protocol.ProtocolServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LineBasedFrameDecoder;

import javax.inject.Inject;
import javax.inject.Named;

@InputPluginScope
public class JsonLineProtocolServer implements ProtocolServer {
    public static final int MAX_LINE = 0xffff;

    private final ObjectMapper mapper;
    private final ChannelInboundHandler handler;
    private final JsonObjectMapperDecoder decoder;

    @Inject
    public JsonLineProtocolServer(
        @Named("application/json") ObjectMapper mapper, InputChannelInboundHandler handler,
        JsonObjectMapperDecoder decoder
    ) {
        this.mapper = mapper;
        this.handler = handler;
        this.decoder = decoder;
    }

    @Override
    public final ChannelInitializer<Channel> initializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new LineBasedFrameDecoder(MAX_LINE));
                ch.pipeline().addLast(decoder, handler);
            }
        };
    }
}
