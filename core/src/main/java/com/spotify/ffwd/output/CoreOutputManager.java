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
package com.spotify.ffwd.output;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.spotify.ffwd.debug.DebugServer;
import com.spotify.ffwd.filter.Filter;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import com.spotify.ffwd.statistics.OutputManagerStatistics;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class CoreOutputManager implements OutputManager {
    private static final String DEBUG_ID = "core.output";

    private final List<PluginSink> sinks;
    private final AsyncFramework async;
    private final Map<String, String> tags;
    private final String host;
    private final long ttl;
    private final DebugServer debug;
    private final OutputManagerStatistics statistics;
    private final Filter filter;

    @Inject
    public CoreOutputManager(
        final List<PluginSink> sinks, final AsyncFramework async,
        @Named("tags") final Map<String, String> tags, @Named("host") final String host,
        @Named("ttl") final long ttl, final DebugServer debug,
        final OutputManagerStatistics statistics, @Named("output") final Filter filter
    ) {
        this.sinks = sinks;
        this.async = async;
        this.tags = tags;
        this.host = host;
        this.ttl = ttl;
        this.debug = debug;
        this.statistics = statistics;
        this.filter = filter;
    }

    @Override
    public void init() {
        log.info("Initializing (filter: {})", filter);

        for (final PluginSink s : sinks) {
            s.init();
        }
    }

    @Override
    public void sendEvent(Event event) {
        if (!filter.matchesEvent(event)) {
            statistics.reportEventsDroppedByFilter(1);
            return;
        }

        statistics.reportSentEvents(1);

        final Event filtered = filter(event);

        debug.inspectEvent(DEBUG_ID, filtered);

        for (final PluginSink s : sinks) {
            if (s.isReady()) {
                s.sendEvent(filtered);
            }
        }
    }

    @Override
    public void sendMetric(Metric metric) {
        if (!filter.matchesMetric(metric)) {
            statistics.reportMetricsDroppedByFilter(1);
            return;
        }

        statistics.reportSentMetrics(1);

        final Metric filtered = filter(metric);

        debug.inspectMetric(DEBUG_ID, filtered);

        for (final PluginSink s : sinks) {
            if (s.isReady()) {
                s.sendMetric(filtered);
            }
        }
    }

    @Override
    public AsyncFuture<Void> start() {
        final ArrayList<AsyncFuture<Void>> futures = Lists.newArrayList();

        for (final PluginSink s : sinks) {
            futures.add(s.start());
        }

        return async.collectAndDiscard(futures);
    }

    @Override
    public AsyncFuture<Void> stop() {
        final ArrayList<AsyncFuture<Void>> futures = Lists.newArrayList();

        for (final PluginSink s : sinks) {
            futures.add(s.stop());
        }

        return async.collectAndDiscard(futures);
    }

    /**
     * Filter the provided Event and complete fields.
     */
    private Event filter(Event event) {
        if (tags.isEmpty() && ttl == 0) {
            return event;
        }

        final String host = event.getHost() != null ? event.getHost() : this.host;
        final Map<String, String> mergedTags = Maps.newHashMap(tags);
        mergedTags.putAll(event.getTags());

        final Date time = event.getTime() != null ? event.getTime() : new Date();
        final Long ttl = event.getTtl() != 0 ? event.getTtl() : this.ttl;

        return new Event(event.getKey(), event.getValue(), time, ttl, event.getState(),
            event.getDescription(), host, event.getRiemannTags(), mergedTags);
    }

    /**
     * Filter the provided Metric and complete fields.
     */
    private Metric filter(Metric metric) {
        if (tags.isEmpty()) {
            return metric;
        }

        final String host = metric.getHost() != null ? metric.getHost() : this.host;

        final Map<String, String> mergedTags = Maps.newHashMap(tags);
        mergedTags.putAll(metric.getTags());

        final Date time = metric.getTime() != null ? metric.getTime() : new Date();

        return new Metric(metric.getKey(), metric.getValue(), time, host, metric.getRiemannTags(),
            mergedTags, metric.getProc());
    }
}
