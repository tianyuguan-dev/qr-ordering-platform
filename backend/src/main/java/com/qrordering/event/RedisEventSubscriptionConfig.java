package com.qrordering.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * Subscribe to Redis channels and dispatch to OrderEventConsumer.
 * Uses raw MessageListener to avoid MessageListenerAdapter invoker issues.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriptionConfig {

    private static final String CHANNEL_ORDER_CREATED = "events:OrderCreatedEvent";
    private static final String CHANNEL_ORDER_STATUS_CHANGED = "events:OrderStatusChangedEvent";

    private final OrderEventConsumer orderEventConsumer;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        MessageListener listener = (message, pattern) -> {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            if (CHANNEL_ORDER_CREATED.equals(channel)) {
                orderEventConsumer.onOrderCreated(json);
            } else if (CHANNEL_ORDER_STATUS_CHANGED.equals(channel)) {
                orderEventConsumer.onOrderStatusChanged(json);
            }
        };

        container.addMessageListener(listener, new ChannelTopic(CHANNEL_ORDER_CREATED));
        container.addMessageListener(listener, new ChannelTopic(CHANNEL_ORDER_STATUS_CHANGED));

        log.info("Subscribed to Redis channels: {}, {}", CHANNEL_ORDER_CREATED, CHANNEL_ORDER_STATUS_CHANGED);
        return container;
    }
}
