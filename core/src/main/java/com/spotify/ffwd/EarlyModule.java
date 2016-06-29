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

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.spotify.ffwd.filter.AndFilter;
import com.spotify.ffwd.filter.Filter;
import com.spotify.ffwd.filter.FilterDeserializer;
import com.spotify.ffwd.filter.MatchKey;
import com.spotify.ffwd.filter.MatchTag;
import com.spotify.ffwd.filter.NotFilter;
import com.spotify.ffwd.filter.OrFilter;
import com.spotify.ffwd.filter.TrueFilter;
import com.spotify.ffwd.filter.TypeFilter;
import com.spotify.ffwd.module.PluginContext;
import com.spotify.ffwd.module.PluginContextImpl;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Module
public class EarlyModule {
    @Provides
    @EarlyScope
    public Map<String, FilterDeserializer.PartialDeserializer> filters() {
        final Map<String, FilterDeserializer.PartialDeserializer> filters = new HashMap<>();
        filters.put("key", new MatchKey.Deserializer());
        filters.put("=", new MatchTag.Deserializer());
        filters.put("true", new TrueFilter.Deserializer());
        filters.put("false", new TrueFilter.Deserializer());
        filters.put("and", new AndFilter.Deserializer());
        filters.put("or", new OrFilter.Deserializer());
        filters.put("not", new NotFilter.Deserializer());
        filters.put("type", new TypeFilter.Deserializer());
        return filters;
    }

    @Provides
    @EarlyScope
    @Named("application/yaml+config")
    public SimpleModule configModule(
        Map<String, FilterDeserializer.PartialDeserializer> filters
    ) {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(Filter.class, new FilterDeserializer(filters));
        return module;
    }

    @Provides
    @EarlyScope
    public PluginContext pluginContext(PluginContextImpl pluginContext) {
        return pluginContext;
    }
}
