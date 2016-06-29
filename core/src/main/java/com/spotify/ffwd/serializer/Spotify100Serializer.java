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
package com.spotify.ffwd.serializer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@JsonTypeName("spotify100")
public class Spotify100Serializer implements Serializer {
    public static final String SCHEMA_VERSION = "1.0.0";

    private final ObjectMapper mapper;

    @Inject
    public Spotify100Serializer(@Named("application/json") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Data
    public static class Spotify100Metric {
        private final String version = SCHEMA_VERSION;
        private final String key;
        private final String host;
        private final Long time;
        private final Map<String, String> attributes;
        private final Double value;
    }

    @Data
    public static class Spotify100Event {
        private final String version = SCHEMA_VERSION;
        private final String key;
        private final String host;
        private final Long time;
        private final Map<String, String> attributes;
        private final Double value;
    }

    @Override
    public byte[] serialize(Event source) throws Exception {
        final Spotify100Event e =
            new Spotify100Event(source.getKey(), source.getHost(), source.getTime().getTime(),
                source.getTags(), source.getValue());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mapper.writeValue(outputStream, e);
        return outputStream.toByteArray();
    }

    @Override
    public byte[] serialize(Metric source) throws Exception {
        final Spotify100Metric m =
            new Spotify100Metric(source.getKey(), source.getHost(), source.getTime().getTime(),
                source.getTags(), source.getValue());
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mapper.writeValue(outputStream, m);
        return outputStream.toByteArray();
    }
}

