package com.example.prop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.code.enabled:false}")
    private boolean codeEnabled;

    @Value("${cache.code.ttl-minutes:10}")
    private long codeTtl;

    @Value("${cache.grp-code.ttl-minutes:30}")
    private long grpCodeTtl;

    @Bean
    public CacheManager cacheManager(
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            RedisConnectionFactory factory) {
        if (!codeEnabled || factory == null) {
            return new ConcurrentMapCacheManager("codes", "grp-codes");
        }

        RedisCacheConfiguration defConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("codes",     defConfig.entryTtl(Duration.ofMinutes(codeTtl)));
        cacheConfigs.put("grp-codes", defConfig.entryTtl(Duration.ofMinutes(grpCodeTtl)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
