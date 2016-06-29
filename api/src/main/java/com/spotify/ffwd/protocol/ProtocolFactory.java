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
package com.spotify.ffwd.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Data type suitable for building using a @JsonCreator block.
 *
 * @author udoprog
 */
@Data
public class ProtocolFactory {
    public static final String DEFAULT_HOST = "127.0.0.1";

    private final Optional<String> type;
    private final Optional<String> host;
    private final Optional<Integer> port;
    private final Optional<Integer> receiveBufferSize;

    @JsonCreator
    public ProtocolFactory(
        @JsonProperty("type") Optional<String> type, @JsonProperty("host") Optional<String> host,
        @JsonProperty("port") Optional<Integer> port,
        @JsonProperty("receiveBufferSize") Optional<Integer> receiveBufferSize
    ) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * Build a default instance of {@link ProtocolFactory}.
     *
     * @return
     */
    public static ProtocolFactory defaultInstance() {
        return new ProtocolFactory(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    }

    /**
     * @see #protocol(ProtocolType, int, String)
     */
    public Protocol protocol(ProtocolType defaultType, int defaultPort) {
        return protocol(defaultType, defaultPort, DEFAULT_HOST);
    }

    /**
     * Build a new protocol instance with the given defaults if they are missing.
     *
     * @param defaultType Default type.
     * @param defaultPort Default port.
     * @param defaultHost Default host.
     * @return
     */
    public Protocol protocol(ProtocolType defaultType, int defaultPort, String defaultHost) {
        final ProtocolType t = parseProtocolType(type, defaultType);
        final InetSocketAddress address = parseSocketAddress(host, port, defaultHost, defaultPort);
        return new Protocol(t, address, receiveBufferSize);
    }

    private InetSocketAddress parseSocketAddress(
        Optional<String> host, Optional<Integer> port, String defaultHost, int defaultPort
    ) {
        final String h = host.orElse(defaultHost);
        final int p = port.orElse(defaultPort);
        return new InetSocketAddress(h, p);
    }

    private ProtocolType parseProtocolType(Optional<String> type, ProtocolType defaultType) {
        return type.map(t -> ProtocolType.valueOf(t.toUpperCase())).orElse(defaultType);
    }
}
