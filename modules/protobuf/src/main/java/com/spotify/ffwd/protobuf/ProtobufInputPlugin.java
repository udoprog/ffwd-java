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
package com.spotify.ffwd.protobuf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotify.ffwd.input.InputPlugin;
import com.spotify.ffwd.input.InputPluginScope;
import com.spotify.ffwd.protocol.Protocol;
import com.spotify.ffwd.protocol.ProtocolFactory;
import com.spotify.ffwd.protocol.ProtocolServer;
import com.spotify.ffwd.protocol.ProtocolType;
import com.spotify.ffwd.protocol.RetryPolicy;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import java.util.Optional;

@Module
public class ProtobufInputPlugin implements InputPlugin {
    private static final ProtocolType DEFAULT_PROTOCOL = ProtocolType.UDP;
    private static final int DEFAULT_PORT = 19091;

    private final Protocol protocol;
    private final RetryPolicy retry;

    @JsonCreator
    public ProtobufInputPlugin(
        @JsonProperty("protocol") Optional<ProtocolFactory> protocol,
        @JsonProperty("retry") Optional<RetryPolicy> retry
    ) {
        this.protocol = protocol
            .orElseGet(ProtocolFactory::defaultInstance)
            .protocol(DEFAULT_PROTOCOL, DEFAULT_PORT);
        this.retry = retry.orElseGet(RetryPolicy.Exponential::new);
    }

    @Provides
    @InputPluginScope
    public Protocol protocol() {
        return protocol;
    }

    @Provides
    @InputPluginScope
    public RetryPolicy retry() {
        return retry;
    }

    @Provides
    @InputPluginScope
    public ProtocolServer parseProtocolServer(
        Lazy<ProtobufFrameProtocolServer> frame,
        Lazy<ProtobufLengthPrefixedProtocolServer> lengthPrefixed
    ) {
        if (protocol.getType() == ProtocolType.UDP) {
            return frame.get();
        }

        if (protocol.getType() == ProtocolType.TCP) {
            return lengthPrefixed.get();
        }

        throw new IllegalArgumentException("Protocol not supported: " + protocol.getType());
    }

    @Override
    public Exposed setup(final Depends depends) {
        return DaggerProtobufInputPluginComponent
            .builder()
            .coreDependencies(depends.core())
            .protobufInputPlugin(this)
            .build();
    }
}
