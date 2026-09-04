package com.example.chatapi.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public record ChatMessage(String user, String text, String serverId, Instant timestamp) {
    public ChatMessage {
        user = requireText(user, "user");
        text = requireText(text, "text");
        serverId = requireText(serverId, "serverId");
        Objects.requireNonNull(timestamp, "timestamp is required");
    }

    public static ChatMessage create(String user, String text, String serverId, Clock clock) {
        return new ChatMessage(user, text, serverId, Instant.now(clock));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
