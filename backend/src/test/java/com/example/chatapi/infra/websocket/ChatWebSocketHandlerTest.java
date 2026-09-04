package com.example.chatapi.infra.websocket;

import com.example.chatapi.application.port.ChatMessagePublisher;
import com.example.chatapi.application.usecase.SendChatMessageUseCase;
import com.example.chatapi.infra.config.ChatProperties;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatMessagePublisher publisher = mock(ChatMessagePublisher.class);
    private final WebSocketSession session = mock(WebSocketSession.class);
    private final ClientSessionRegistry registry = new ClientSessionRegistry(objectMapper);
    private final ChatWebSocketHandler handler = new ChatWebSocketHandler(
            objectMapper,
            new SendChatMessageUseCase(publisher, "Server-1", Clock.systemUTC()),
            registry,
            new ChatProperties("Server-1", "chat:messages"));

    @BeforeEach
    void setUp() {
        when(session.getId()).thenReturn("connection-1");
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void sendsServerInfoWhenConnectionIsEstablished() throws Exception {
        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(argThat(message -> message instanceof TextMessage textMessage
                && textMessage.getPayload().contains("\"type\":\"SERVER_INFO\"")
                && textMessage.getPayload().contains("\"serverId\":\"Server-1\"")));
    }

    @Test
    void publishesValidInboundMessage() throws Exception {
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session,
                new TextMessage("{\"type\":\"SEND_MESSAGE\",\"user\":\"Alice\",\"message\":\"Olá\"}"));

        verify(publisher).publish(org.mockito.ArgumentMatchers.argThat(message ->
                message.user().equals("Alice") && message.text().equals("Olá")));
    }

    @Test
    void returnsErrorAndDoesNotPublishMalformedJson() throws Exception {
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("not-json"));

        var messages = org.mockito.Mockito.mockingDetails(session).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("sendMessage"))
                .map(invocation -> ((TextMessage) invocation.getArgument(0)).getPayload())
                .toList();
        assertThat(messages).anyMatch(payload -> payload.contains("INVALID_MESSAGE"));
        assertThat(org.mockito.Mockito.mockingDetails(publisher).getInvocations()).isEmpty();
    }
}
