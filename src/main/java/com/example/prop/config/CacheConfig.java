package com.example.prop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.code.detail.ttl-minutes:10}")
    private long detailTtl;

    @Value("${cache.code.grp.ttl-minutes:30}")
    private long grpTtl;

    @Autowired
    private Environment env;

    /**
     * CacheManager wiring:
     *
     *   factory == null (no Redis infrastructure)
     *     → ConcurrentMapCacheManager (JVM-local, permanent fallback)
     *
     *   factory present
     *     → RuntimeToggleCacheManager wrapping RedisCacheManager
     *       · valkey.enabled / cache.code.enabled checked at RUNTIME (env.getProperty)
     *       · flag=false → delegates to JVM-local ConcurrentMap
     *       · flag=true  → Redis wrapped in FallbackCache (absorbs connection/data errors)
     *
     * Flags can be stored in zt_property_source and reloaded without restart.
     */
    @Bean
    public CacheManager cacheManager(
            @Autowired(required = false) RedisConnectionFactory factory) {

        ConcurrentMapCacheManager localManager = new ConcurrentMapCacheManager("detail", "grp");

        if (factory == null) {
            log.warn("No RedisConnectionFactory — code cache uses JVM-local ConcurrentMap");
            return localManager;
        }

        // Keys: code:{cacheName}:{key}  e.g. code:grp:all, code:detail:USER_STATUS
        RedisCacheConfiguration defConfig = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> "code:" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("detail", defConfig.entryTtl(Duration.ofMinutes(detailTtl)));
        cacheConfigs.put("grp",    defConfig.entryTtl(Duration.ofMinutes(grpTtl)));

        RedisCacheManager redisManager = RedisCacheManager.builder(factory)
                .cacheDefaults(defConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();

        return new RuntimeToggleCacheManager(redisManager, localManager, env);
    }
}
