package com.example.chatapi.infra.config;

import com.example.chatapi.application.port.ChatMessagePublisher;
import com.example.chatapi.application.port.ConnectedClientBroadcaster;
import com.example.chatapi.application.usecase.BroadcastReceivedMessageUseCase;
import com.example.chatapi.application.usecase.SendChatMessageUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(ChatProperties.class)
public class ApplicationConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SendChatMessageUseCase sendChatMessageUseCase(
            ChatMessagePublisher publisher, ChatProperties properties, Clock clock) {
        return new SendChatMessageUseCase(publisher, properties.serverId(), clock);
    }

    @Bean
    BroadcastReceivedMessageUseCase broadcastReceivedMessageUseCase(
            ConnectedClientBroadcaster broadcaster) {
        return new BroadcastReceivedMessageUseCase(broadcaster);
    }
}
