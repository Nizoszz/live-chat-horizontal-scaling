package com.example.chatapi.infra.websocket;

import com.example.chatapi.application.port.ConnectedClientBroadcaster;
import com.example.chatapi.domain.ChatMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClientSessionRegistry implements ConnectedClientBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionRegistry.class);
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ClientSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES));
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public void sendServerInfo(String sessionId, String serverId) throws IOException {
        send(sessionId, new ServerInfoMessage("SERVER_INFO", serverId, sessionId));
    }

    public void sendError(String sessionId, String code, String message) throws IOException {
        send(sessionId, new ErrorMessage("ERROR", code, message));
    }

    @Override
    public void broadcast(ChatMessage message) {
        var outbound = new ReceivedMessage(
                "RECEIVE_MESSAGE", message.user(), message.text(), message.serverId(), message.timestamp());
        sessions.keySet().forEach(sessionId -> {
            try {
                send(sessionId, outbound);
            } catch (IOException exception) {
                LOGGER.debug("Removing unavailable WebSocket session {}", sessionId, exception);
                closeAndRemove(sessionId);
            }
        });
    }

    private void send(String sessionId, Object payload) throws IOException {
        var session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            closeAndRemove(sessionId);
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize WebSocket message", exception);
        }
    }

    private void closeAndRemove(String sessionId) {
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
