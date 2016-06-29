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
package com.spotify.ffwd.generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotify.ffwd.input.InputPlugin;
import com.spotify.ffwd.input.InputPluginScope;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import java.util.Optional;

@Module
public class GeneratedInputPlugin implements InputPlugin {
    private final boolean sameHost;

    @JsonCreator
    public GeneratedInputPlugin(@JsonProperty("sameHost") Optional<Boolean> sameHost) {
        this.sameHost = sameHost.orElse(false);
    }

    @Provides
    @InputPluginScope
    @Named("sameHost")
    public boolean sameHost() {
        return sameHost;
    }

    @Override
    public Exposed setup(final Depends depends) {
        return DaggerGeneratedInputPluginComponent
            .builder()
            .coreDependencies(depends.core())
            .generatedInputPlugin(this)
            .build();
    }
}
