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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.spotify.ffwd.AppScope;
import com.spotify.ffwd.CoreDependencies;
import com.spotify.ffwd.InternalCoreDependencies;
import com.spotify.ffwd.filter.Filter;
import com.spotify.ffwd.filter.TrueFilter;
import com.spotify.ffwd.statistics.CoreStatistics;
import com.spotify.ffwd.statistics.InputManagerStatistics;
import dagger.Module;
import dagger.Provides;
import io.netty.channel.ChannelInboundHandler;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Module
public class InputManagerModule {
    private static final List<InputPlugin> DEFAULT_PLUGINS = Lists.newArrayList();

    private final List<InputPlugin> plugins;
    private final Filter filter;

    @JsonCreator
    public InputManagerModule(
        @JsonProperty("plugins") List<InputPlugin> plugins, @JsonProperty("filter") Filter filter
    ) {
        this.plugins = Optional.ofNullable(plugins).orElse(DEFAULT_PLUGINS);
        this.filter = Optional.ofNullable(filter).orElseGet(TrueFilter::new);
    }

    @Provides
    @AppScope
    public InputManagerStatistics statistics(CoreStatistics statistics) {
        return statistics.newInputManager();
    }

    @Provides
    @AppScope
    public List<PluginSource> sources(InternalCoreDependencies core) {
        final List<PluginSource> sources = new ArrayList<>();

        for (final InputPlugin input : plugins) {
            final InputPlugin.Exposed exposed = input.setup(new InputPlugin.Depends() {
                @Override
                public CoreDependencies core() {
                    return core;
                }
            });

            sources.add(exposed.source());
        }

        return Collections.unmodifiableList(sources);
    }

    @Provides
    @AppScope
    @Named("input")
    public Filter filter() {
        return filter;
    }

    @Provides
    @AppScope
    public ChannelInboundHandler inboundHandler(InputChannelInboundHandler inboundHandler) {
        return inboundHandler;
    }

    @Provides
    @AppScope
    public InputManager inputManager(CoreInputManager inputManager) {
        return inputManager;
    }

    public static Supplier<InputManagerModule> supplyDefault() {
        return () -> new InputManagerModule(null, null);
    }
}
