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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.spotify.ffwd.AgentConfig;
import com.spotify.ffwd.AppScope;
import com.spotify.ffwd.CoreDependencies;
import com.spotify.ffwd.InternalCoreDependencies;
import com.spotify.ffwd.filter.Filter;
import com.spotify.ffwd.filter.TrueFilter;
import com.spotify.ffwd.statistics.CoreStatistics;
import com.spotify.ffwd.statistics.OutputManagerStatistics;
import dagger.Module;
import dagger.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Module
public class OutputManagerModule {
    private static final List<OutputPlugin> DEFAULT_PLUGINS = Lists.newArrayList();

    private final List<OutputPlugin> plugins;
    private final Filter filter;

    @JsonCreator
    public OutputManagerModule(
        @JsonProperty("plugins") List<OutputPlugin> plugins, @JsonProperty("filter") Filter filter
    ) {
        this.plugins = Optional.ofNullable(plugins).orElse(DEFAULT_PLUGINS);
        this.filter = Optional.ofNullable(filter).orElseGet(TrueFilter::new);
    }

    @Provides
    @AppScope
    public OutputManagerStatistics statistics(CoreStatistics statistics) {
        return statistics.newOutputManager();
    }

    @Provides
    @AppScope
    public List<PluginSink> sinks(InternalCoreDependencies core) {
        final List<PluginSink> sinks = new ArrayList<>();

        for (final OutputPlugin output : plugins) {
            final OutputPlugin.Exposed exposed = output.setup(new OutputPlugin.Depends() {
                @Override
                public Logger logger() {
                    return LoggerFactory.getLogger(output.toString());
                }

                @Override
                public CoreDependencies core() {
                    return core;
                }
            });

            sinks.add(exposed.sink());
        }

        return Collections.unmodifiableList(sinks);
    }

    @Provides
    @AppScope
    @Named("tags")
    public Map<String, String> tags(AgentConfig config) {
        return config.getTags();
    }

    @Provides
    @AppScope
    @Named("host")
    public String host(AgentConfig config) {
        return config.getHost();
    }

    @Provides
    @AppScope
    @Named("ttl")
    public long ttl(AgentConfig config) {
        return config.getTtl();
    }

    @Provides
    @AppScope
    @Named("output")
    public Filter filter() {
        return filter;
    }

    @Provides
    @AppScope
    public OutputManager outputManager(CoreOutputManager outputManager) {
        return outputManager;
    }

    public static Supplier<OutputManagerModule> supplyDefault() {
        return () -> new OutputManagerModule(null, null);
    }
}
