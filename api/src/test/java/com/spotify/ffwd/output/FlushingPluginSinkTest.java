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

import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import com.spotify.ffwd.statistics.OutputPluginStatistics;
import eu.toolchain.async.AsyncFramework;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FlushingPluginSinkTest {
    private final FlushConfig flushConfig =
        new FlushConfig(Optional.empty(), Optional.empty(), Optional.empty());

    @Mock
    private AsyncFramework async;

    @Mock
    private BatchedPluginSink childSink;

    @Mock
    private Metric metric;

    @Mock
    private Event event;

    @Mock
    private FlushingPluginSink.Batch batch;

    @Mock
    private Logger log;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private OutputPluginStatistics statistics;

    private FlushingPluginSink sink;

    @Before
    public void setup() {
        sink =
            spy(new FlushingPluginSink(async, childSink, scheduler, log, statistics, flushConfig));
    }

    @Test
    public void testSendMetric() {
        assertEquals(0, sink.nextBatch.size());
        doNothing().when(sink).checkBatch(sink.nextBatch);

        sink.sendMetric(metric);

        assertEquals(1, sink.nextBatch.size());
        verify(sink).checkBatch(sink.nextBatch);
    }

    @Test
    public void testSendMetricDrop() {
        sink.nextBatch = null;

        doNothing().when(sink).checkBatch(sink.nextBatch);

        sink.sendEvent(event);

        verify(sink, never()).checkBatch(sink.nextBatch);
    }

    @Test
    public void testSendEvent() {
        assertEquals(0, sink.nextBatch.size());
        doNothing().when(sink).checkBatch(sink.nextBatch);

        sink.sendEvent(event);

        assertEquals(1, sink.nextBatch.size());
        verify(sink).checkBatch(sink.nextBatch);
    }

    @Test
    public void testSendEventDrop() {
        sink.nextBatch = null;

        doNothing().when(sink).checkBatch(sink.nextBatch);

        sink.sendEvent(event);

        verify(sink, never()).checkBatch(sink.nextBatch);
    }

    @Test
    public void testCheckBatchFlushes() {
        doReturn((int) FlushingPluginSink.DEFAULT_BATCH_SIZE_LIMIT).when(batch).size();
        doNothing().when(sink).flushNowThenScheduleNext();

        sink.checkBatch(batch);

        verify(sink).flushNowThenScheduleNext();
    }

    @Test
    public void testCheckBatchDoesntFlush() {
        doReturn((int) FlushingPluginSink.DEFAULT_BATCH_SIZE_LIMIT - 1).when(batch).size();
        doNothing().when(sink).flushNowThenScheduleNext();

        sink.checkBatch(batch);

        verify(sink, never()).flushNowThenScheduleNext();
    }
}