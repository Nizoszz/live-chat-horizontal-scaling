package com.example.chatapi.infra.websocket;

record ServerInfoMessage(String type, String serverId, String connectionId) {
}
