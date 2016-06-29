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
package com.spotify.ffwd.debug;

import com.spotify.ffwd.AgentConfig;
import dagger.Module;
import dagger.Provides;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.RequiredArgsConstructor;

import javax.inject.Named;
import java.net.InetSocketAddress;

@RequiredArgsConstructor
@Module
public class NettyDebugServerModule {
    private final AgentConfig.Debug config;

    @Provides
    @DebugScope
    @Named("localAddress")
    public InetSocketAddress localAddress() {
        return config.getLocalAddress();
    }

    @Provides
    @DebugScope
    @Named("boss")
    public EventLoopGroup boss() {
        return new NioEventLoopGroup(2);
    }

    @Provides
    @DebugScope
    @Named("worker")
    public EventLoopGroup worker() {
        return new NioEventLoopGroup(4);
    }
}
