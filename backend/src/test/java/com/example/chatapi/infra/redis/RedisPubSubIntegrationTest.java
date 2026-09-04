package com.example.chatapi.infra.redis;

import com.example.chatapi.application.usecase.BroadcastReceivedMessageUseCase;
import com.example.chatapi.domain.ChatMessage;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisPubSubIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
            .withExposedPorts(6379);

    @Test
    void publishesAndReceivesMessageThroughRedis() throws Exception {
        var connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        var container = new RedisMessageListenerContainer();
        try {
            var received = new LinkedBlockingQueue<ChatMessage>();
            var mapper = new ObjectMapper();
            var subscriber = new RedisChatMessageSubscriber(
                    mapper, new BroadcastReceivedMessageUseCase(received::offer));
            var topic = ChannelTopic.of("chat:messages:test");
            container.setConnectionFactory(connectionFactory);
            container.addMessageListener(subscriber, topic);
            container.afterPropertiesSet();
            container.start();

            var publisher = new RedisChatMessagePublisher(
                    new StringRedisTemplate(connectionFactory), mapper, topic);
            var expected = new ChatMessage("Alice", "Olá", "Server-1", Instant.now());
            publisher.publish(expected);

            assertThat(received.poll(5, TimeUnit.SECONDS)).isEqualTo(expected);
        } finally {
            container.stop();
            connectionFactory.destroy();
        }
    }
}
