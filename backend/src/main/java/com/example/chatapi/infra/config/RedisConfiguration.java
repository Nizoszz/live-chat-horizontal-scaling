package com.example.chatapi.infra.config;

import com.example.chatapi.infra.redis.RedisChatMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfiguration {
    @Bean
    ChannelTopic chatTopic(ChatProperties properties) {
        return ChannelTopic.of(properties.redisChannel());
    }

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisChatMessageSubscriber subscriber,
            ChannelTopic chatTopic) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, chatTopic);
        return container;
    }
}
