package com.spotify.ffwd.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Batch {
    private final Map<String, String> commonTags;
    private final List<Metric> metrics;

    @JsonCreator
    public Batch(
        @JsonProperty("commonTags") final Map<String, String> commonTags,
        @JsonProperty("metrics") final List<Metric> metrics
    ) {
        this.commonTags = commonTags;
        this.metrics = metrics;
    }

    @Data
    public static class Metric {
        private final String key;
        private final Map<String, String> tags;
        private final double value;
        private final long timestamp;

        public Metric(
            @JsonProperty("key") final String key,
            @JsonProperty("tags") final Map<String, String> tags,
            @JsonProperty("value") final double value,
            @JsonProperty("timestamp") final long timestamp
        ) {
            this.key = key;
            this.tags = tags;
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
