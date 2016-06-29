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
package com.spotify.ffwd.protocol;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.Timer;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

public class ProtocolServersImpl implements ProtocolServers {
    private final AsyncFramework async;
    private final EventLoopGroup boss;
    private final EventLoopGroup worker;
    private final Timer timer;

    @Inject
    public ProtocolServersImpl(
        AsyncFramework async, @Named("worker") EventLoopGroup worker,
        @Named("boss") EventLoopGroup boss, Timer timer
    ) {
        this.async = async;
        this.worker = worker;
        this.boss = boss;
        this.timer = timer;
    }

    @Override
    public AsyncFuture<ProtocolConnection> bind(
        Logger log, Protocol protocol, ProtocolServer server, RetryPolicy policy
    ) {
        if (protocol.getType() == ProtocolType.UDP) {
            return bindUDP(log, protocol, server, policy);
        }

        if (protocol.getType() == ProtocolType.TCP) {
            return bindTCP(log, protocol, server, policy);
        }

        throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }

    private AsyncFuture<ProtocolConnection> bindTCP(
        final Logger log, final Protocol protocol, ProtocolServer server, RetryPolicy policy
    ) {
        final ServerBootstrap b = new ServerBootstrap();

        b.group(boss, worker);
        b.channel(NioServerSocketChannel.class);
        b.childHandler(server.initializer());

        b.option(ChannelOption.SO_BACKLOG, 128);

        protocol.getReceiveBufferSize().ifPresent(r -> b.childOption(ChannelOption.SO_RCVBUF, r));

        b.childOption(ChannelOption.SO_KEEPALIVE, true);

        final String host = protocol.getAddress().getHostString();
        final int port = protocol.getAddress().getPort();

        final RetryingProtocolConnection connection =
            new RetryingProtocolConnection(async, timer, log, policy, new ProtocolChannelSetup() {
                @Override
                public ChannelFuture setup() {
                    return b.bind(host, port);
                }

                @Override
                public String toString() {
                    return String.format("bind tcp://%s:%d", host, port);
                }
            });

        return connection.getInitialFuture();
    }

    private AsyncFuture<ProtocolConnection> bindUDP(
        final Logger log, final Protocol protocol, ProtocolServer server, RetryPolicy policy
    ) {
        final Bootstrap b = new Bootstrap();

        b.group(worker);
        b.channel(NioDatagramChannel.class);
        b.handler(server.initializer());

        protocol.getReceiveBufferSize().ifPresent(r -> {
            b.option(ChannelOption.SO_RCVBUF, r);
        });

        final String host = protocol.getAddress().getHostString();
        final int port = protocol.getAddress().getPort();

        final RetryingProtocolConnection connection =
            new RetryingProtocolConnection(async, timer, log, policy, new ProtocolChannelSetup() {
                @Override
                public ChannelFuture setup() {
                    return b.bind(host, port);
                }

                @Override
                public String toString() {
                    return String.format("bind tcp://%s:%d", host, port);
                }
            });

        return connection.getInitialFuture();
    }
}
