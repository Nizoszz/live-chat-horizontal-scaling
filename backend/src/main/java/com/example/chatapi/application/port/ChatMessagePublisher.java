package com.example.chatapi.application.port;

import com.example.chatapi.domain.ChatMessage;

public interface ChatMessagePublisher {
    void publish(ChatMessage message);
}
