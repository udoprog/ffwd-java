package com.spotify.ffwd.snoop;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import com.spotify.ffwd.snoop.api.Websocket;
import com.spotify.ffwd.snoop.api.WebsocketManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class SnoopData implements WebsocketManager{
    private final Map<Event, Event> events;
    private final Map<Metric, Metric> metrics;
    private final Set<Websocket> sockets;
    private final JsonFactory jsonFactory;

    public SnoopData(ObjectMapper objectMapper) {
        this.jsonFactory = objectMapper.getFactory();
        events = new HashMap<>();
        metrics = new HashMap<>();
        sockets = new HashSet<>();
    }

    private String buildUpdateMessage(Collection<Metric> metrics,
                                      Collection<Event> events) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final JsonGenerator jsonGenerator = jsonFactory.createGenerator(os);
        jsonGenerator.writeStartObject();
        if (!metrics.isEmpty()){
            jsonGenerator.writeFieldName("metrics");
            jsonGenerator.writeStartObject();
            for(final Metric metric: metrics) {
                jsonGenerator.writeObjectField(Integer.toHexString(metric.hashCode()), metric);
            }
            jsonGenerator.writeEndObject();
        }
        if (!events.isEmpty()){
            jsonGenerator.writeFieldName("events");
            jsonGenerator.writeStartObject();
            for(final Event event: this.events.values()) {
                jsonGenerator.writeObjectField(Integer.toHexString(event.hashCode()), event);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
        jsonGenerator.close();
        return new String(os.toByteArray(), "UTF-8");
    }

    public void join(Websocket socket) {
        sockets.add(socket);
        try {
            final String message = buildUpdateMessage(this.metrics.values(),  this.events.values());
            socket.send(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leave(Websocket socket) {
        sockets.remove(socket);
    }


    public void updateMetrics(Collection<Metric> updatedMetrics){
        updatedMetrics.forEach(m->metrics.put(m, m));
        try {
            final String message = buildUpdateMessage(updatedMetrics, Collections.EMPTY_LIST);
            sockets.forEach(s -> s.send(message));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateMetric(Metric updatedMetric){
        updateMetrics(Collections.singletonList(updatedMetric));
    }

    public void updateEvents(Collection<Event> updatedEvents){
        updatedEvents.forEach(e->events.put(e, e));
        try {
            final String message = buildUpdateMessage(Collections.EMPTY_LIST, updatedEvents);
            sockets.forEach(s -> s.send(message));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateEvent(Event updatedEvent){
        updateEvents(Collections.singletonList(updatedEvent));
    }

}
