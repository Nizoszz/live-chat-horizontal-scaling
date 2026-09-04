package com.example.chatapi.infra.websocket;

record InboundMessage(MessageType type, String user, String message) {
}
