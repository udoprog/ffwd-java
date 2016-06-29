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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.Lists;
import com.spotify.ffwd.debug.DaggerNettyDebugServerComponent;
import com.spotify.ffwd.debug.DaggerNoopDebugServerComponent;
import com.spotify.ffwd.debug.DebugServer;
import com.spotify.ffwd.debug.DebugServerComponent;
import com.spotify.ffwd.debug.NettyDebugServerModule;
import com.spotify.ffwd.input.InputManager;
import com.spotify.ffwd.input.InputManagerModule;
import com.spotify.ffwd.module.FastForwardModule;
import com.spotify.ffwd.output.OutputManager;
import com.spotify.ffwd.output.OutputManagerModule;
import com.spotify.ffwd.statistics.CoreStatistics;
import com.spotify.ffwd.statistics.NoopCoreStatistics;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AgentCore {
    private final List<Class<? extends FastForwardModule>> modules;
    private final Path config;
    private final CoreStatistics statistics;

    private AgentCore(
        final List<Class<? extends FastForwardModule>> modules, Path config,
        CoreStatistics statistics
    ) {
        this.modules = modules;
        this.config = config;
        this.statistics = statistics;
    }

    public void run() throws Exception {
        final InternalEarlyDependencies early = setupEarlyInjector();
        final AgentConfig config = readConfig(early);
        final InternalCoreDependencies core = setupCore(early, config);
        final InternalAppDependencies app = setupApp(core, config);

        start(app);
        log.info("Started!");

        waitUntilStopped(app);
        log.info("Stopped, Bye Bye!");
    }

    private void waitUntilStopped(final InternalAppDependencies app) throws InterruptedException {
        final CountDownLatch shutdown = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(setupShutdownHook(app, shutdown));
        shutdown.await();
    }

    private Thread setupShutdownHook(
        final InternalAppDependencies app, final CountDownLatch shutdown
    ) {
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    AgentCore.this.stop(app);
                } catch (Exception e) {
                    log.error("AgentCore#stop(Injector) failed", e);
                }

                shutdown.countDown();
            }
        };

        thread.setName("ffwd-agent-core-shutdown-hook");

        return thread;
    }

    private void start(final InternalAppDependencies app) throws Exception {
        final InputManager input = app.inputManager();
        final OutputManager output = app.outputManager();
        final DebugServer debug = app.debugServer();

        // Bind InputManager to the InputChannel.
        final CoreInputChannel inputChannel = (CoreInputChannel) app.inputChannel();
        inputChannel.setInput(input);

        final AsyncFramework async = app.async();
        final ArrayList<AsyncFuture<Void>> startup = Lists.newArrayList();

        log.info("Waiting for all components to start...");

        startup.add(output.start());
        startup.add(input.start());
        startup.add(debug.start());

        async.collectAndDiscard(startup).get(10, TimeUnit.SECONDS);

        input.init();
        output.init();
    }

    private void stop(final InternalAppDependencies primary) throws Exception {
        final InputManager input = primary.inputManager();
        final OutputManager output = primary.outputManager();
        final DebugServer debug = primary.debugServer();
        final AsyncFramework async = primary.async();

        final ArrayList<AsyncFuture<Void>> shutdown = Lists.newArrayList();

        log.info("Waiting for all components to stop...");

        shutdown.add(input.stop());
        shutdown.add(output.stop());
        shutdown.add(debug.stop());

        AsyncFuture<Void> all = async.collectAndDiscard(shutdown);

        try {
            all.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            log.error("All components did not stop in a timely fashion", e);
            all.cancel();
        }
    }

    /**
     * Setup early application Injector.
     * <p>
     * The early injector is used by modules to configure the system.
     *
     * @throws Exception If something could not be set up.
     */
    private InternalEarlyDependencies setupEarlyInjector() throws Exception {
        final EarlyModule earlyModule = new EarlyModule();

        final InternalEarlyDependencies early =
            DaggerInternalEarlyDependencies.builder().earlyModule(earlyModule).build();

        for (final FastForwardModule m : loadModules(early)) {
            log.info("Setting up {}", m);

            try {
                m.setup(early);
            } catch (Exception e) {
                throw new Exception("Failed to call #setup() for module: " + m, e);
            }
        }

        return early;
    }

    /**
     * Setup primary Injector.
     *
     * @return The primary injector.
     */
    private InternalCoreDependencies setupCore(
        final InternalEarlyDependencies early, final AgentConfig config
    ) {
        final InputManagerModule inputManager = config.getInput();
        final OutputManagerModule outputManager = config.getOutput();
        final CoreModule core = new CoreModule(statistics, config);

        return DaggerInternalCoreDependencies
            .builder()
            .coreModule(core)
            .internalEarlyDependencies(early)
            .build();
    }

    private InternalAppDependencies setupApp(
        final InternalCoreDependencies core, final AgentConfig config
    ) {
        final DebugServerComponent debugServer = config.getDebug().<DebugServerComponent>map(d -> {
            return DaggerNettyDebugServerComponent
                .builder()
                .coreDependencies(core)
                .nettyDebugServerModule(new NettyDebugServerModule(d))
                .build();
        }).orElseGet(() -> {
            return DaggerNoopDebugServerComponent.builder().coreDependencies(core).build();
        });

        final AppModule app = new AppModule();

        return DaggerInternalAppDependencies
            .builder()
            .appModule(app)
            .inputManagerModule(config.getInput())
            .outputManagerModule(config.getOutput())
            .debugServerComponent(debugServer)
            .internalCoreDependencies(core)
            .build();
    }

    private AgentConfig readConfig(EarlyDependencies early) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final SimpleModule module = early.configModule();

        mapper.registerModule(module);
        mapper.registerModule(new Jdk8Module());

        try (final InputStream input = Files.newInputStream(this.config)) {
            return mapper.readValue(input, AgentConfig.class);
        } catch (JsonParseException | JsonMappingException e) {
            throw new IOException("Failed to parse configuration", e);
        }
    }

    private List<FastForwardModule> loadModules(EarlyDependencies early) throws Exception {
        final List<FastForwardModule> modules = Lists.newArrayList();

        for (final Class<? extends FastForwardModule> module : this.modules) {
            final Constructor<? extends FastForwardModule> constructor;

            try {
                constructor = module.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new Exception("Expected empty constructor for class: " + module, e);
            }

            final FastForwardModule m;

            try {
                m = constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new Exception("Failed to call constructor for class: " + module, e);
            }

            modules.add(m);
        }

        return modules;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<Class<? extends FastForwardModule>> modules = Lists.newArrayList();
        private Path config = Paths.get("ffwd.yaml");
        private CoreStatistics statistics = NoopCoreStatistics.get();

        public Builder config(Path config) {
            if (config == null) {
                throw new NullPointerException("'config' must not be null");
            }

            this.config = config;
            return this;
        }

        public Builder modules(List<Class<? extends FastForwardModule>> modules) {
            if (modules == null) {
                throw new NullPointerException("'modules' must not be null");
            }

            this.modules = modules;
            return this;
        }

        public Builder statistics(CoreStatistics statistics) {
            if (statistics == null) {
                throw new NullPointerException("'statistics' most not be null");
            }

            this.statistics = statistics;
            return this;
        }

        public AgentCore build() {
            return new AgentCore(modules, config, statistics);
        }
    }
}
