// $LICENSE
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
package com.spotify.ffwd.snoop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.spotify.ffwd.output.OutputPlugin;
import com.spotify.ffwd.output.PluginSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SnoopOutputPlugin implements OutputPlugin {
    private static final int DEFAULT_PORT = 8080;

    private final Integer port;


    @JsonCreator
    public SnoopOutputPlugin(@JsonProperty("port") final int port) {
        this.port = Optional.fromNullable(port).or(DEFAULT_PORT);
    }

    @Override
    public Module module(final Key<PluginSink> key, final String id) {
        return new PrivateModule() {
            @Override
            protected void configure() {
                bind(Logger.class).toInstance(LoggerFactory.getLogger(id));
                bind(key).toInstance(new SnoopPluginSink(port));
                expose(key);
            }
        };
    }

    @Override
    public String id(int index) {
        return port.toString();
    }
}
