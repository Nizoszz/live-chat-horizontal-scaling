package com.example.chatapi.application.usecase;

import com.example.chatapi.application.port.ChatMessagePublisher;
import com.example.chatapi.domain.ChatMessage;

import java.time.Clock;

public final class SendChatMessageUseCase {
    private final ChatMessagePublisher publisher;
    private final String serverId;
    private final Clock clock;

    public SendChatMessageUseCase(ChatMessagePublisher publisher, String serverId, Clock clock) {
        this.publisher = publisher;
        this.serverId = serverId;
        this.clock = clock;
    }

    public void execute(String user, String text) {
        publisher.publish(ChatMessage.create(user, text, serverId, clock));
    }
}
