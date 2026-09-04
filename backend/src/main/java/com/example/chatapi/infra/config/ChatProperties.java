package com.example.chatapi.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat")
public record ChatProperties(String serverId, String redisChannel) {
}
