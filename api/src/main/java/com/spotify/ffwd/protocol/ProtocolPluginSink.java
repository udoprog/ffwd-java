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

import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import com.spotify.ffwd.output.BatchedPluginSink;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.LazyTransform;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class ProtocolPluginSink implements BatchedPluginSink {
    private final AtomicReference<ProtocolConnection> connection = new AtomicReference<>();

    private final AsyncFramework async;
    private final ProtocolClients clients;
    private final Protocol protocol;
    private final ProtocolClient client;
    private final Logger log;
    private final RetryPolicy retry;

    @Inject
    public ProtocolPluginSink(
        final AsyncFramework async, final ProtocolClients clients, final Protocol protocol,
        final ProtocolClient client, final Logger log, final RetryPolicy retry
    ) {
        this.async = async;
        this.clients = clients;
        this.protocol = protocol;
        this.client = client;
        this.log = log;
        this.retry = retry;
    }

    @Override
    public void init() {
    }

    @Override
    public void sendEvent(Event event) {
        final ProtocolConnection c = connection.get();

        if (c == null) {
            return;
        }

        c.send(event);
    }

    @Override
    public void sendMetric(Metric metric) {
        final ProtocolConnection c = connection.get();

        if (c == null) {
            return;
        }

        c.send(metric);
    }

    @Override
    public AsyncFuture<Void> sendEvents(Collection<Event> events) {
        final ProtocolConnection c = connection.get();

        if (c == null) {
            return async.failed(new IllegalStateException("not connected to " + protocol));
        }

        return c.sendAll(events);
    }

    @Override
    public AsyncFuture<Void> sendMetrics(Collection<Metric> metrics) {
        final ProtocolConnection c = connection.get();

        if (c == null) {
            return async.failed(new IllegalStateException("not connected to " + protocol));
        }

        return c.sendAll(metrics);
    }

    @Override
    public AsyncFuture<Void> start() {
        return clients
            .connect(log, protocol, client, retry)
            .lazyTransform(new LazyTransform<ProtocolConnection, Void>() {
                @Override
                public AsyncFuture<Void> transform(ProtocolConnection result) throws Exception {
                    if (!connection.compareAndSet(null, result)) {
                        return result.stop();
                    }

                    return async.resolved(null);
                }
            });
    }

    @Override
    public AsyncFuture<Void> stop() {
        final ProtocolConnection c = connection.getAndSet(null);

        if (c == null) {
            return async.resolved(null);
        }

        return c.stop();
    }

    @Override
    public boolean isReady() {
        final ProtocolConnection c = connection.get();

        if (c == null) {
            return false;
        }

        return c.isConnected();
    }
}
