package com.example.chatapi.infra.websocket;

import com.example.chatapi.application.port.ConnectedClientBroadcaster;
import com.example.chatapi.domain.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebSocketClientBroadcaster implements ConnectedClientBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClientBroadcaster.class);

    private final ClientSessionRegistry sessions;
    private final WebSocketMessageSender sender;

    public WebSocketClientBroadcaster(ClientSessionRegistry sessions, WebSocketMessageSender sender) {
        this.sessions = sessions;
        this.sender = sender;
    }

    @Override
    public void broadcast(ChatMessage message) {
        var outbound = new ReceivedMessage(
                MessageType.RECEIVE_MESSAGE,
                message.user(),
                message.text(),
                message.serverId(),
                message.timestamp());
        sessions.sessionIds().forEach(sessionId -> {
            try {
                sender.send(sessionId, outbound);
            } catch (IOException exception) {
                LOGGER.debug("Removing unavailable WebSocket session {}", sessionId, exception);
                sessions.closeAndRemove(sessionId);
            }
        });
    }
}
