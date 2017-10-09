package com.spotify.ffwd.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Batch {
    private final Map<String, String> commonTags;

    private final List<Metric> metrics;

    private final List<Event> events;

    @JsonCreator
    public Batch(
        @JsonProperty("commonTags") final Map<String, String> commonTags,
        @JsonProperty("metrics") final List<Metric> metrics,
        @JsonProperty("events") final List<Event> events) {

        this.commonTags = commonTags;
        this.metrics = metrics;
        this.events = events;
    }
}
