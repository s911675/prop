package com.example.prop.property;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes a reload notification to all instances via Redis Pub/Sub.
 *
 * Bean existence: gated by cache.property.pubsub.enabled=true at startup
 *                 (prevents Redis connection attempt in test / no-Redis envs).
 *
 * Runtime toggle: even when the bean exists, publish() checks valkey.enabled
 *                 and cache.property.pubsub.enabled from Environment on every call.
 *                 Flags stored in zt_property_source take effect after reload
 *                 without application restart.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cache.property.pubsub.enabled", havingValue = "true")
public class PropertyReloadPublisher {

    static final String CHANNEL = "prop:reload";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Environment env;

    public void publish() {
        if (!isEnabled()) {
            log.debug("Pub/Sub publish skipped (valkey.enabled or cache.property.pubsub.enabled is false)");
            return;
        }
        try {
            redisTemplate.convertAndSend(CHANNEL, "reload");
            log.debug("Published reload event to channel '{}'", CHANNEL);
        } catch (Exception e) {
            log.warn("Failed to publish reload event (local reload already done): {}", e.getMessage());
        }
    }

    private boolean isEnabled() {
        return "true".equalsIgnoreCase(env.getProperty("valkey.enabled",                "true"))
            && "true".equalsIgnoreCase(env.getProperty("cache.property.pubsub.enabled", "true"));
    }
}
