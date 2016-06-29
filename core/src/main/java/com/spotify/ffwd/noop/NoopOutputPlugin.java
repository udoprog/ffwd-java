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
package com.spotify.ffwd.noop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotify.ffwd.output.BatchedPluginSink;
import com.spotify.ffwd.output.FlushConfig;
import com.spotify.ffwd.output.FlushingPluginSink;
import com.spotify.ffwd.output.OutputPlugin;
import com.spotify.ffwd.output.OutputPluginScope;
import com.spotify.ffwd.output.PluginSink;
import com.spotify.ffwd.statistics.CoreStatistics;
import com.spotify.ffwd.statistics.OutputPluginStatistics;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Module
public class NoopOutputPlugin implements OutputPlugin {
    public static final String DEFAULT_ID = "noop";

    private static final long DEFAULT_FLUSH_INTERVAL =
        TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);

    private final String id;
    private final Optional<FlushConfig> flushConfig;

    @JsonCreator
    public NoopOutputPlugin(
        @JsonProperty("id") Optional<String> id,
        @JsonProperty("flush") Optional<FlushConfig> flushConfig
    ) {
        this.id = id.orElse(DEFAULT_ID);
        this.flushConfig = flushConfig;
    }

    @Provides
    @OutputPluginScope
    public Logger log() {
        return LoggerFactory.getLogger(log.getName() + "[" + id + "]");
    }

    @Provides
    @OutputPluginScope
    public FlushConfig flushConfig() {
        return flushConfig.orElseThrow(() -> new IllegalStateException("flush: not configured"));
    }

    @Provides
    @OutputPluginScope
    public BatchedPluginSink batchedPluginSink(NoopPluginSink noop) {
        return noop;
    }

    @Provides
    @OutputPluginScope
    public PluginSink sink(Lazy<FlushingPluginSink> flushing, Lazy<NoopPluginSink> direct) {
        if (flushConfig.isPresent()) {
            return flushing.get();
        }

        return direct.get();
    }

    @Provides
    @OutputPluginScope
    public OutputPluginStatistics statistics(CoreStatistics core) {
        return core.newOutputPlugin(id);
    }

    @Override
    public Exposed setup(final Depends depends) {
        return DaggerNoopOutputPluginComponent
            .builder()
            .coreDependencies(depends.core())
            .noopOutputPlugin(this)
            .build();
    }
}
