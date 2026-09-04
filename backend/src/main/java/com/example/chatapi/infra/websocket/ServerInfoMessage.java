package com.example.chatapi.infra.websocket;

record ServerInfoMessage(MessageType type, String serverId, String connectionId) {
}
