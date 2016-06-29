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
package com.spotify.ffwd;

import com.spotify.ffwd.input.InputChannel;
import com.spotify.ffwd.input.InputManager;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;

import javax.inject.Inject;

@CoreScope
public class CoreInputChannel implements InputChannel {
    private volatile InputManager input;

    @Inject
    public CoreInputChannel() {
    }

    @Override
    public void receiveEvent(final Event event) {
        input().receiveEvent(event);
    }

    @Override
    public void receiveMetric(final Metric metric) {
        input().receiveMetric(metric);
    }

    public void setInput(final InputManager input) {
        this.input = input;
    }

    private InputManager input() {
        final InputManager in = this.input;

        if (in == null) {
            throw new IllegalStateException("InputManager is not set");
        }

        return in;
    }
}
