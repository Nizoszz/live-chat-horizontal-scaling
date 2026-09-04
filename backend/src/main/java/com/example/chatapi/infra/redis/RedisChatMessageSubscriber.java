package com.example.chatapi.infra.redis;

import com.example.chatapi.application.usecase.BroadcastReceivedMessageUseCase;
import com.example.chatapi.domain.ChatMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisChatMessageSubscriber implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisChatMessageSubscriber.class);

    private final ObjectMapper objectMapper;
    private final BroadcastReceivedMessageUseCase useCase;

    public RedisChatMessageSubscriber(ObjectMapper objectMapper, BroadcastReceivedMessageUseCase useCase) {
        this.objectMapper = objectMapper;
        this.useCase = useCase;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            useCase.execute(objectMapper.readValue(message.getBody(), ChatMessage.class));
        } catch (JacksonException | IllegalArgumentException exception) {
            LOGGER.warn("Discarding invalid message received from Redis", exception);
        } catch (RuntimeException exception) {
            LOGGER.error("Could not broadcast message received from Redis", exception);
        }
    }
}
