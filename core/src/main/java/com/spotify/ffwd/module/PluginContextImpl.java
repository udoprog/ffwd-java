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
package com.spotify.ffwd.module;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.spotify.ffwd.input.InputPlugin;
import com.spotify.ffwd.output.OutputPlugin;
import com.spotify.ffwd.serializer.Serializer;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
public class PluginContextImpl implements PluginContext {
    private final SimpleModule module;

    @Inject
    public PluginContextImpl(@Named("application/yaml+config") SimpleModule module) {
        this.module = module;
    }

    @Override
    public void registerInput(String name, Class<? extends InputPlugin> input) {
        module.registerSubtypes(new NamedType(input, name));
    }

    @Override
    public void registerOutput(String name, Class<? extends OutputPlugin> output) {
        module.registerSubtypes(new NamedType(output, name));
    }

    @Override
    public void registerSerializer(String name, Class<? extends Serializer> serializer) {
        module.registerSubtypes(new NamedType(serializer, name));
    }
}
