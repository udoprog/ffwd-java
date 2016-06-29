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
package com.spotify.ffwd.json;

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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Slf4j
@Module
public class JsonInputPlugin implements InputPlugin {
    private static final ProtocolType DEFAULT_PROTOCOL = ProtocolType.UDP;
    private static final int DEFAULT_PORT = 19000;

    private static final String FRAME = "frame";
    private static final String LINE = "line";

    public static final String DEFAULT_DELIMITER = FRAME;

    private final Protocol protocol;
    private final String delimiter;
    private final RetryPolicy retry;

    @JsonCreator
    public JsonInputPlugin(
        @JsonProperty("protocol") Optional<ProtocolFactory> protocol,
        @JsonProperty("delimiter") Optional<String> delimiter,
        @JsonProperty("retry") Optional<RetryPolicy> retry
    ) {
        this.protocol = protocol
            .orElseGet(ProtocolFactory::defaultInstance)
            .protocol(DEFAULT_PROTOCOL, DEFAULT_PORT);
        this.delimiter = delimiter.orElseGet(this::defaultDelimiter);
        this.retry = retry.orElseGet(RetryPolicy.Exponential::new);
    }

    private String defaultDelimiter() {
        if (protocol.getType() == ProtocolType.TCP) {
            return LINE;
        }

        if (protocol.getType() == ProtocolType.UDP) {
            return FRAME;
        }

        return DEFAULT_DELIMITER;
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
    public ProtocolServer protocolServer(
        Lazy<JsonFrameProtocolServer> frame, Lazy<JsonLineProtocolServer> line
    ) {
        if (FRAME.equals(delimiter)) {
            if (protocol.getType() == ProtocolType.TCP) {
                throw new IllegalArgumentException("frame-based decoding is not suitable for TCP");
            }

            return frame.get();
        }

        if (LINE.equals(delimiter)) {
            return line.get();
        }

        if (protocol.getType() == ProtocolType.TCP) {
            return line.get();
        }

        return frame.get();
    }

    @Provides
    @InputPluginScope
    public Logger log() {
        return LoggerFactory.getLogger(log.getName() + "[" + protocol + "]");
    }

    @Override
    public Exposed setup(final Depends depends) {
        return DaggerJsonInputPluginComponent
            .builder()
            .coreDependencies(depends.core())
            .jsonInputPlugin(this)
            .build();
    }
}
