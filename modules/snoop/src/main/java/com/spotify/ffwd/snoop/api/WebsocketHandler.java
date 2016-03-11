package com.spotify.ffwd.snoop.api;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.servlet.*;

@Slf4j
public class WebsocketHandler extends WebSocketServlet {
    private final WebsocketManager websocketManager;

    public WebsocketHandler(WebsocketManager websocketManager) {
        super();
        this.websocketManager = websocketManager;
    }

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.setCreator(new WebsocketCreator(websocketManager));
    }

    static class WebsocketCreator implements WebSocketCreator {
        final private WebsocketManager websocketManager;

        public WebsocketCreator(WebsocketManager websocketManager) {
            this.websocketManager = websocketManager;
        }

        @Override
        public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
                                      ServletUpgradeResponse servletUpgradeResponse) {
            return new Websocket(websocketManager);
        }
    }
}
