package com.example.prop.property;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a reload notification to all instances via Redis Pub/Sub.
 * Active only when cache.property.pubsub.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cache.property.pubsub.enabled", havingValue = "true")
public class PropertyReloadPublisher {

    static final String CHANNEL = "prop:reload";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void publish() {
        try {
            redisTemplate.convertAndSend(CHANNEL, "reload");
            log.debug("Published reload event to channel '{}'", CHANNEL);
        } catch (Exception e) {
            log.warn("Failed to publish reload event to Redis (local reload already done): {}", e.getMessage());
        }
    }
}
