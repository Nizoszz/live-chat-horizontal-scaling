package com.example.chatapi.infra.websocket;

import com.example.chatapi.domain.ChatMessage;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketClientBroadcasterTest {
    private final ClientSessionRegistry sessions = new ClientSessionRegistry();
    private final WebSocketMessageSender sender = new WebSocketMessageSender(sessions, new ObjectMapper());
    private final WebSocketClientBroadcaster broadcaster = new WebSocketClientBroadcaster(sessions, sender);

    @Test
    void broadcastsMessageToEveryConnectedSession() throws Exception {
        var first = openSession("connection-1");
        var second = openSession("connection-2");
        sessions.register(first);
        sessions.register(second);

        broadcaster.broadcast(new ChatMessage(
                "Alice", "Olá", "Server-1", Instant.parse("2026-09-04T12:00:00Z")));

        org.mockito.ArgumentMatcher<TextMessage> expectedPayload = message ->
                message.getPayload().contains("\"type\":\"RECEIVE_MESSAGE\"")
                        && message.getPayload().contains("\"user\":\"Alice\"");
        verify(first).sendMessage(argThat(expectedPayload));
        verify(second).sendMessage(argThat(expectedPayload));
    }

    @Test
    void removesAndClosesSessionWhenBroadcastFails() throws Exception {
        var session = openSession("connection-1");
        doThrow(new IOException("send failed")).when(session).sendMessage(org.mockito.ArgumentMatchers.any());
        sessions.register(session);

        broadcaster.broadcast(new ChatMessage(
                "Alice", "Olá", "Server-1", Instant.parse("2026-09-04T12:00:00Z")));

        verify(session).close();
        assertThat(sessions.find("connection-1")).isNull();
    }

    private WebSocketSession openSession(String id) {
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
