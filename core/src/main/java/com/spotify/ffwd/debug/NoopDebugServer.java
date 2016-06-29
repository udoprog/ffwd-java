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

import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;

import javax.inject.Inject;

public class NoopDebugServer implements DebugServer {
    private final AsyncFramework async;

    @Inject
    public NoopDebugServer(final AsyncFramework async) {
        this.async = async;
    }

    @Override
    public void inspectEvent(String id, Event event) {
    }

    @Override
    public void inspectMetric(String id, Metric metric) {
    }

    @Override
    public AsyncFuture<Void> start() {
        return async.resolved();
    }

    @Override
    public AsyncFuture<Void> stop() {
        return async.resolved();
    }
}
