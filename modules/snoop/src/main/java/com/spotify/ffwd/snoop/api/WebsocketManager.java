package com.spotify.ffwd.snoop.api;

public interface WebsocketManager {
    void join(Websocket socket);
    void leave(Websocket socket);
}
