package com.example.chatapi.infra.redis;

import com.example.chatapi.application.port.ChatMessagePublisher;
import com.example.chatapi.domain.ChatMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class RedisChatMessagePublisher implements ChatMessagePublisher {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic topic;

    public RedisChatMessagePublisher(
            StringRedisTemplate redisTemplate, ObjectMapper objectMapper, ChannelTopic topic) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(ChatMessage message) {
        try {
            redisTemplate.convertAndSend(topic.getTopic(), objectMapper.writeValueAsString(message));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize chat message", exception);
        }
    }
}
