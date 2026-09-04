package com.example.chatapi.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {
    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void createsNormalizedMessage() {
        var message = ChatMessage.create(" Alice ", " Olá ", "Server-1", CLOCK);

        assertThat(message.user()).isEqualTo("Alice");
        assertThat(message.text()).isEqualTo("Olá");
        assertThat(message.timestamp()).isEqualTo(NOW);
    }

    @Test
    void rejectsBlankFields() {
        assertThatThrownBy(() -> ChatMessage.create(" ", "Olá", "Server-1", CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatMessage.create("Alice", " ", "Server-1", CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatMessage.create("Alice", "Olá", " ", CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
