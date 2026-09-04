package com.example.chatapi.infra.websocket;

record InboundMessage(String type, String user, String message) {
}
