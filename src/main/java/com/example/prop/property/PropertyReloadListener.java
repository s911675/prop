package com.example.prop.property;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Listens to the "prop:reload" Redis channel and reloads the local PropertySource.
 * This keeps all instances in sync when any instance writes to the property table.
 *
 * Note: the publishing instance also receives its own message, causing a second
 * reload. This is harmless — reload() is synchronized and idempotent.
 * To skip self-messages, embed an instance ID in the payload and filter here.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cache.property.pubsub.enabled", havingValue = "true")
public class PropertyReloadListener implements MessageListener {

    @Autowired
    private DatabasePropertySourceConfigurer configurer;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("Received reload event on channel '{}' — reloading PropertySource", PropertyReloadPublisher.CHANNEL);
        try {
            int keys = configurer.reload();
            log.info("PropertySource reloaded via pub/sub: {} keys", keys);
        } catch (Exception e) {
            log.error("PropertySource reload via pub/sub failed", e);
        }
    }

    @Bean
    public RedisMessageListenerContainer reloadListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(this, new ChannelTopic(PropertyReloadPublisher.CHANNEL));
        return container;
    }
}
