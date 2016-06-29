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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.spotify.ffwd.input.InputPluginScope;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Sharable
@InputPluginScope
public class JsonObjectMapperDecoder extends MessageToMessageDecoder<ByteBuf> {
    public static final Set<String> EMPTY_TAGS = Sets.newHashSet();
    public static final Map<String, String> EMPTY_ATTRIBUTES = new HashMap<>();

    private final ObjectMapper mapper;

    @Inject
    public JsonObjectMapperDecoder(
        @Named("application/json") ObjectMapper mapper
    ) {
        this.mapper = mapper;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
        throws Exception {
        if (!in.isReadable()) {
            return;
        }

        final Object frame;

        try {
            frame = decode0(in, out);
        } catch (Exception e) {
            log.error("Discarding invalid frame", e);
            return;
        }

        out.add(frame);
    }

    private Object decode0(ByteBuf in, List<Object> out)
        throws IOException, JsonProcessingException {
        final JsonNode tree;

        try (final InputStream input = new ByteBufInputStream(in)) {
            tree = mapper.readTree(input);
        }

        final JsonNode typeNode = tree.get("type");

        if (typeNode == null) {
            throw new IllegalArgumentException("Missing field 'type'");
        }

        final String type = typeNode.asText();

        if ("event".equals(type)) {
            return decodeEvent(tree, out);
        }

        if ("metric".equals(type)) {
            return decodeMetric(tree, out);
        }

        throw new IllegalArgumentException("Invalid metric type '" + type + "'");
    }

    private Object decodeMetric(JsonNode tree, List<Object> out) {
        final String key = decodeString(tree, "key");
        final double value = decodeDouble(tree, "value");
        final Date time = decodeTime(tree, "time");
        final String host = decodeString(tree, "host");
        final Set<String> riemannTags = decodeTags(tree, "tags");
        final Map<String, String> tags = decodeAttributes(tree, "attributes");
        final String proc = decodeString(tree, "proc");

        return new Metric(key, value, time, host, riemannTags, tags, proc);
    }

    private Object decodeEvent(JsonNode tree, List<Object> out) {
        final String key = decodeString(tree, "key");
        final double value = decodeDouble(tree, "value");
        final Date time = decodeTime(tree, "time");
        final long ttl = decodeTtl(tree, "ttl");
        final String state = decodeString(tree, "state");
        final String description = decodeString(tree, "description");
        final String host = decodeString(tree, "host");
        final Set<String> riemannTags = decodeTags(tree, "tags");
        final Map<String, String> tags = decodeAttributes(tree, "attributes");

        return new Event(key, value, time, ttl, state, description, host, riemannTags, tags);
    }

    private long decodeTtl(JsonNode tree, String name) {
        final JsonNode n = tree.get(name);

        if (n == null) {
            return 0;
        }

        return n.asLong();
    }

    private Date decodeTime(JsonNode tree, String name) {
        final JsonNode n = tree.get(name);

        if (n == null) {
            return null;
        }

        final long time = n.asLong();
        return new Date(time);
    }

    private double decodeDouble(JsonNode tree, String name) {
        final JsonNode n = tree.get(name);

        if (n == null) {
            return Double.NaN;
        }

        return n.asDouble();
    }

    private String decodeString(JsonNode tree, String name) {
        final JsonNode n = tree.get(name);

        if (n == null) {
            return null;
        }

        return n.asText();
    }

    private Map<String, String> decodeAttributes(JsonNode tree, String name) {
        final JsonNode n = tree.get(name);

        if (n == null) {
            return EMPTY_ATTRIBUTES;
        }

        if (n.getNodeType() != JsonNodeType.OBJECT) {
            return EMPTY_ATTRIBUTES;
        }

        final Map<String, String> attributes = Maps.newHashMap();

        final Iterator<Map.Entry<String, JsonNode>> iter = n.fields();

        while (iter.hasNext()) {
            final Map.Entry<String, JsonNode> e = iter.next();
            attributes.put(e.getKey(), e.getValue().asText());
        }

        return attributes;
    }

    private Set<String> decodeTags(JsonNode tree, String name) {
        final JsonNode n = tree.get(name);

        if (n == null) {
            return EMPTY_TAGS;
        }

        if (n.getNodeType() != JsonNodeType.ARRAY) {
            return EMPTY_TAGS;
        }

        final List<String> tags = Lists.newArrayList();

        final Iterator<JsonNode> iter = n.elements();

        while (iter.hasNext()) {
            tags.add(iter.next().asText());
        }

        return Sets.newHashSet(tags);
    }
}
