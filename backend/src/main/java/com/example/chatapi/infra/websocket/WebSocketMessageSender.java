package com.example.chatapi.infra.websocket;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;

@Component
public class WebSocketMessageSender {
    private final ClientSessionRegistry sessions;
    private final ObjectMapper objectMapper;

    public WebSocketMessageSender(ClientSessionRegistry sessions, ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.objectMapper = objectMapper;
    }

    public void sendServerInfo(String sessionId, String serverId) throws IOException {
        send(sessionId, new ServerInfoMessage(MessageType.SERVER_INFO, serverId, sessionId));
    }

    public void sendError(String sessionId, ErrorCode code, String message) throws IOException {
        send(sessionId, new ErrorMessage(MessageType.ERROR, code, message));
    }

    void send(String sessionId, Object payload) throws IOException {
        var session = sessions.find(sessionId);
        if (session == null || !session.isOpen()) {
            sessions.closeAndRemove(sessionId);
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize WebSocket message", exception);
        }
    }
}
