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
package com.spotify.ffwd.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotify.ffwd.output.OutputPlugin;
import com.spotify.ffwd.output.OutputPluginScope;
import com.spotify.ffwd.protocol.Protocol;
import com.spotify.ffwd.protocol.ProtocolClient;
import com.spotify.ffwd.protocol.ProtocolFactory;
import com.spotify.ffwd.protocol.ProtocolType;
import com.spotify.ffwd.protocol.RetryPolicy;
import dagger.Module;
import dagger.Provides;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Slf4j
@Module
public class TemplateOutputPlugin implements OutputPlugin {
    public static final String DEFAULT_ID = "template";
    private static final ProtocolType DEFAULT_PROTOCOL = ProtocolType.TCP;
    private static final int DEFAULT_PORT = 8910;

    private final String id;
    private final Protocol protocol;
    private final RetryPolicy retry;

    @JsonCreator
    public TemplateOutputPlugin(
        @JsonProperty("id") final Optional<String> id,
        @JsonProperty("protocol") final Optional<ProtocolFactory> protocol,
        @JsonProperty("retry") final Optional<RetryPolicy> retry
    ) {
        this.id = id.orElse(DEFAULT_ID);
        this.protocol = protocol
            .orElseGet(ProtocolFactory::defaultInstance)
            .protocol(DEFAULT_PROTOCOL, DEFAULT_PORT);
        this.retry = retry.orElseGet(RetryPolicy.Exponential::new);
    }

    @Provides
    @OutputPluginScope
    public Protocol protocol() {
        return protocol;
    }

    @Provides
    @OutputPluginScope
    public RetryPolicy retry() {
        return retry;
    }

    @Provides
    @OutputPluginScope
    public ProtocolClient client(TemplateOutputProtocolClient client) {
        return client;
    }

    @Provides
    @OutputPluginScope
    public Logger log() {
        return LoggerFactory.getLogger(log.getName() + "[" + id + "/" + protocol + "]");
    }

    @Override
    public Exposed setup(final Depends depends) {
        return DaggerTemplateOutputPluginComponent
            .builder()
            .coreDependencies(depends.core())
            .templateOutputPlugin(this)
            .build();
    }
}
