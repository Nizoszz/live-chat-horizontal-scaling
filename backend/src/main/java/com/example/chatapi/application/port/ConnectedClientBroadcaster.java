package com.example.chatapi.application.port;

import com.example.chatapi.domain.ChatMessage;

public interface ConnectedClientBroadcaster {
    void broadcast(ChatMessage message);
}
