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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Timer;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

public class ProtocolClientsImpl implements ProtocolClients {
    private final AsyncFramework async;
    private final EventLoopGroup worker;
    private final Timer timer;

    @Inject
    public ProtocolClientsImpl(
        AsyncFramework async, @Named("worker") EventLoopGroup worker, Timer timer
    ) {
        this.async = async;
        this.worker = worker;
        this.timer = timer;
    }

    @Override
    public AsyncFuture<ProtocolConnection> connect(
        Logger log, Protocol protocol, ProtocolClient client, RetryPolicy policy
    ) {
        if (protocol.getType() == ProtocolType.UDP) {
            return connectUDP(protocol, client, policy);
        }

        if (protocol.getType() == ProtocolType.TCP) {
            return connectTCP(log, protocol, client, policy);
        }

        throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }

    private AsyncFuture<ProtocolConnection> connectTCP(
        Logger log, Protocol protocol, ProtocolClient client, RetryPolicy policy
    ) {
        final Bootstrap b = new Bootstrap();

        b.group(worker);
        b.channel(NioSocketChannel.class);
        b.handler(client.initializer());

        b.option(ChannelOption.SO_KEEPALIVE, true);

        final String host = protocol.getAddress().getHostString();
        final int port = protocol.getAddress().getPort();

        final ProtocolConnection connection =
            new RetryingProtocolConnection(async, timer, log, policy, new ProtocolChannelSetup() {
                @Override
                public ChannelFuture setup() {
                    return b.connect(host, port);
                }

                @Override
                public String toString() {
                    return String.format("connect tcp://%s:%d", host, port);
                }
            });

        return async.resolved(connection);
    }

    private AsyncFuture<ProtocolConnection> connectUDP(
        Protocol protocol, ProtocolClient client, RetryPolicy policy
    ) {
        return async.failed(new RuntimeException("not implemented"));
    }
}
