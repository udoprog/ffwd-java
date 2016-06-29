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
package com.spotify.ffwd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.spotify.ffwd.input.InputChannel;
import com.spotify.ffwd.protocol.ProtocolClients;
import com.spotify.ffwd.protocol.ProtocolClientsImpl;
import com.spotify.ffwd.protocol.ProtocolServers;
import com.spotify.ffwd.protocol.ProtocolServersImpl;
import com.spotify.ffwd.statistics.CoreStatistics;
import dagger.Module;
import dagger.Provides;
import eu.toolchain.async.AsyncCaller;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.DirectAsyncCaller;
import eu.toolchain.async.TinyAsync;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

@RequiredArgsConstructor
@Slf4j
@Module
public class CoreModule {
    private final CoreStatistics statistics;
    private final AgentConfig config;

    @CoreScope
    @Provides
    public CoreStatistics statistics() {
        return statistics;
    }

    @CoreScope
    @Provides
    public ExecutorService executor() {
        final ThreadFactory factory =
            new ThreadFactoryBuilder().setNameFormat("ffwd-async-%d").build();
        return Executors.newFixedThreadPool(config.getAsyncThreads(), factory);
    }

    @CoreScope
    @Provides
    public ScheduledExecutorService scheduledExecutor() {
        final ThreadFactory factory =
            new ThreadFactoryBuilder().setNameFormat("ffwd-scheduler-%d").build();
        return Executors.newScheduledThreadPool(config.getSchedulerThreads(), factory);
    }

    @CoreScope
    @Provides
    public AsyncFramework async(ExecutorService executor) {
        final AsyncCaller caller = new DirectAsyncCaller() {
            @Override
            protected void internalError(String what, Throwable e) {
                log.error("Async call '{}' failed", what, e);
            }
        };

        return TinyAsync.builder().executor(executor).caller(caller).build();
    }

    @CoreScope
    @Provides
    @Named("boss")
    public EventLoopGroup bosses() {
        final ThreadFactory factory =
            new ThreadFactoryBuilder().setNameFormat("ffwd-boss-%d").build();
        return new NioEventLoopGroup(config.getBossThreads(), factory);
    }

    @CoreScope
    @Provides
    @Named("worker")
    public EventLoopGroup workers() {
        final ThreadFactory factory =
            new ThreadFactoryBuilder().setNameFormat("ffwd-worker-%d").build();
        return new NioEventLoopGroup(config.getWorkerThreads(), factory);
    }

    @CoreScope
    @Provides
    @Named("application/json")
    public ObjectMapper jsonMapper() {
        final ObjectMapper m = new ObjectMapper();
        m.registerModule(new Jdk8Module());
        return m;
    }

    @CoreScope
    @Provides
    public AgentConfig config() {
        return config;
    }

    @CoreScope
    @Provides
    public Timer timer() {
        return new HashedWheelTimer();
    }

    @CoreScope
    @Provides
    public ProtocolServers servers(ProtocolServersImpl servers) {
        return servers;
    }

    @CoreScope
    @Provides
    public ProtocolClients clients(ProtocolClientsImpl clients) {
        return clients;
    }

    @CoreScope
    @Provides
    public InputChannel input(CoreInputChannel input) {
        return input;
    }
}
