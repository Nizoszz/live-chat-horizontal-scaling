package com.example.chatapi.application.usecase;

import com.example.chatapi.application.port.ChatMessagePublisher;
import com.example.chatapi.domain.ChatMessage;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendChatMessageUseCaseTest {
    private final AtomicReference<ChatMessage> published = new AtomicReference<>();
    private final ChatMessagePublisher publisher = published::set;
    private final SendChatMessageUseCase useCase = new SendChatMessageUseCase(
            publisher,
            "Server-2",
            Clock.fixed(Instant.parse("2026-09-04T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void enrichesAndPublishesMessage() {
        useCase.execute("Alice", "Olá");

        assertThat(published.get().serverId()).isEqualTo("Server-2");
        assertThat(published.get().text()).isEqualTo("Olá");
    }

    @Test
    void doesNotPublishInvalidMessage() {
        assertThatThrownBy(() -> useCase.execute("Alice", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(published.get()).isNull();
    }
}
