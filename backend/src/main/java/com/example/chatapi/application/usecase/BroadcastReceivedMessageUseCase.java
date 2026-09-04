package com.example.chatapi.application.usecase;

import com.example.chatapi.application.port.ConnectedClientBroadcaster;
import com.example.chatapi.domain.ChatMessage;

public final class BroadcastReceivedMessageUseCase {
    private final ConnectedClientBroadcaster broadcaster;

    public BroadcastReceivedMessageUseCase(ConnectedClientBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void execute(ChatMessage message) {
        broadcaster.broadcast(message);
    }
}
