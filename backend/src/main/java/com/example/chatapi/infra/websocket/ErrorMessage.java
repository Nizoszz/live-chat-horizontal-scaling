package com.example.chatapi.infra.websocket;

record ErrorMessage(MessageType type, ErrorCode code, String message) {
}
