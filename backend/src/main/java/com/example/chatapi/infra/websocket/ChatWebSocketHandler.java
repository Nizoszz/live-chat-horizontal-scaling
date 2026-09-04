package com.example.chatapi.infra.websocket;

import com.example.chatapi.application.usecase.SendChatMessageUseCase;
import com.example.chatapi.infra.config.ChatProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final SendChatMessageUseCase sendMessage;
    private final ClientSessionRegistry sessions;
    private final ChatProperties properties;

    public ChatWebSocketHandler(
            ObjectMapper objectMapper,
            SendChatMessageUseCase sendMessage,
            ClientSessionRegistry sessions,
            ChatProperties properties) {
        this.objectMapper = objectMapper;
        this.sendMessage = sendMessage;
        this.sessions = sessions;
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessions.register(session);
        sessions.sendServerInfo(session.getId(), properties.serverId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            var inbound = objectMapper.readValue(message.getPayload(), InboundMessage.class);
            if (!"SEND_MESSAGE".equals(inbound.type())) {
                sessions.sendError(session.getId(), "UNSUPPORTED_MESSAGE_TYPE", "Expected SEND_MESSAGE");
                return;
            }
            sendMessage.execute(inbound.user(), inbound.message());
        } catch (IllegalArgumentException exception) {
            sessions.sendError(session.getId(), "INVALID_MESSAGE", exception.getMessage());
        } catch (JacksonException exception) {
            LOGGER.debug("Invalid WebSocket payload", exception);
            sessions.sendError(session.getId(), "INVALID_MESSAGE", "Malformed JSON payload");
        } catch (RuntimeException exception) {
            LOGGER.error("Could not process WebSocket message", exception);
            sessions.sendError(session.getId(), "MESSAGE_PROCESSING_FAILED", "Message could not be processed");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.unregister(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        LOGGER.debug("WebSocket transport error for session {}", session.getId(), exception);
        sessions.unregister(session.getId());
    }
}
