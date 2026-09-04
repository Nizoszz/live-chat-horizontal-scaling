package com.example.chatapi.infra.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClientSessionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionRegistry.class);
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    public void register(WebSocketSession session) {
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES));
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    WebSocketSession find(String sessionId) {
        return sessions.get(sessionId);
    }

    Collection<String> sessionIds() {
        return List.copyOf(sessions.keySet());
    }

    void closeAndRemove(String sessionId) {
        var session = sessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException exception) {
                LOGGER.debug("Could not close WebSocket session {}", sessionId, exception);
            }
        }
    }
}
