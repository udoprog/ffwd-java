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
package com.spotify.ffwd.output;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Optional;

@Data
public class FlushConfig {
    private final Optional<Long> flushInterval;
    private final Optional<Long> batchSizeLimit;
    private final Optional<Long> maxPendingFlushes;

    @JsonCreator
    public FlushConfig(
        @JsonProperty("flushInterval") Optional<Long> flushInterval,
        @JsonProperty("batchSizeLimit") Optional<Long> batchSizeLimit,
        @JsonProperty("maxPendingFlushes") Optional<Long> maxPendingFlushes
    ) {
        this.flushInterval = flushInterval;
        this.batchSizeLimit = batchSizeLimit;
        this.maxPendingFlushes = maxPendingFlushes;
    }
}
