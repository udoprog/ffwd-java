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
package com.spotify.ffwd.input;

import com.google.common.collect.Lists;
import com.spotify.ffwd.debug.DebugServer;
import com.spotify.ffwd.filter.Filter;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import com.spotify.ffwd.output.OutputManager;
import com.spotify.ffwd.statistics.InputManagerStatistics;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import lombok.ToString;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for receiving, logging and transforming the event.
 *
 * @author udoprog
 */
@ToString(of = {"sinks"})
public class CoreInputManager implements InputManager {
    private static final String DEBUG_ID = "core.input";
    private final List<PluginSource> sources;
    private final AsyncFramework async;
    private final OutputManager output;
    private final DebugServer debug;
    private final InputManagerStatistics statistics;
    private final Filter filter;

    @Inject
    public CoreInputManager(
        final List<PluginSource> sources, final AsyncFramework async, final OutputManager output,
        final DebugServer debug, final InputManagerStatistics statistics,
        @Named("input") final Filter filter
    ) {
        this.sources = sources;
        this.async = async;
        this.output = output;
        this.debug = debug;
        this.statistics = statistics;
        this.filter = filter;
    }

    @Override
    public void init() {
        for (final PluginSource s : sources) {
            s.init();
        }
    }

    @Override
    public void receiveEvent(Event event) {
        if (!filter.matchesEvent(event)) {
            statistics.reportEventsDroppedByFilter(1);
            return;
        }

        statistics.reportReceivedEvents(1);
        debug.inspectEvent(DEBUG_ID, event);
        output.sendEvent(event);
    }

    @Override
    public void receiveMetric(Metric metric) {
        if (!filter.matchesMetric(metric)) {
            statistics.reportMetricsDroppedByFilter(1);
            return;
        }

        statistics.reportReceivedMetrics(1);
        debug.inspectMetric(DEBUG_ID, metric);
        output.sendMetric(metric);
    }

    @Override
    public AsyncFuture<Void> start() {
        final ArrayList<AsyncFuture<Void>> futures = Lists.newArrayList();

        for (final PluginSource s : sources) {
            futures.add(s.start());
        }

        return async.collectAndDiscard(futures);
    }

    @Override
    public AsyncFuture<Void> stop() {
        final ArrayList<AsyncFuture<Void>> futures = Lists.newArrayList();

        for (final PluginSource s : sources) {
            futures.add(s.stop());
        }

        return async.collectAndDiscard(futures);
    }
}
