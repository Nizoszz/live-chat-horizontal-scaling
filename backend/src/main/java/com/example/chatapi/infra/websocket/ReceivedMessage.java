package com.example.chatapi.infra.websocket;

import java.time.Instant;

record ReceivedMessage(MessageType type, String user, String text, String serverId, Instant timestamp) {
}
