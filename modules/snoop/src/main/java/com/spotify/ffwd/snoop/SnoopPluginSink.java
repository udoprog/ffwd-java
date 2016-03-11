// $LICENSE
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
package com.spotify.ffwd.snoop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import com.spotify.ffwd.output.BatchedPluginSink;
import com.spotify.ffwd.snoop.api.WebsocketHandler;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Callable;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

@Slf4j
class SnoopPluginSink implements BatchedPluginSink {
    @Inject
    private AsyncFramework async;

    private final Server server;
    private final SnoopData snoopData;

    public SnoopPluginSink(Integer port) {
        final ObjectMapper objectMapper = new ObjectMapper();
        this.snoopData = new SnoopData(objectMapper);
        final WebsocketHandler websocketHandler = new WebsocketHandler(snoopData);
        this.server = new Server(port);

        // statically provide injector to jersey application.
        final ServletContextHandler APIContext =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        APIContext.setContextPath("/");

        final ServletHolder wsHolder = new ServletHolder(websocketHandler);
        APIContext.addServlet(wsHolder, "/stream/*");

        // Request logging
        final RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(new Slf4jRequestLog());

        // Serve static files
        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setBaseResource(Resource.newResource(getClass().getResource("app")));

        // Combine them all into a handler
        final HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[]{resourceHandler, APIContext, requestLogHandler});

        server.setHandler(handlers);
    }

    @Override
    public void init() {
    }

    @Override
    public void sendEvent(final Event event) {
        snoopData.updateEvent(event);
    }

    @Override
    public void sendMetric(final Metric metric) {
        snoopData.updateMetric(metric);
    }

    @Override
    public AsyncFuture<Void> sendEvents(final Collection<Event> events) {
        snoopData.updateEvents(events);
    }

    @Override
    public AsyncFuture<Void> sendMetrics(final Collection<Metric> metrics) {
        snoopData.updateMetrics(metrics);
    }

    @Override
    public AsyncFuture<Void> start() {
        return async.call(() -> {
            server.start();
            return null;
        });
    }

    @Override
    public AsyncFuture<Void> stop() {
        return async.call(() -> {
            server.stop();
            server.join();
            return null;
        });
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
