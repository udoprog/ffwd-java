package com.spotify.ffwd.snoop.api;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@Slf4j
@WebSocket
public class Websocket {
    private Session session;
    private final WebsocketManager websocketManager;

    Websocket(WebsocketManager websocketManager) {
        this.websocketManager = websocketManager;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        websocketManager.join(this);
        log.debug(session.toString());
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        log.debug(message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        websocketManager.leave(this);
        log.debug(Integer.toString(statusCode));
        log.debug(reason);
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        error.printStackTrace();
    }

    public void send(String message) {
        if (session.isOpen()) {
            session.getRemote().sendStringByFuture(message);
        }
    }
}
