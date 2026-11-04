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

    @Value("${cache.code.detail.ttl-minutes:10}")
    private long detailTtl;

    @Value("${cache.code.grp.ttl-minutes:30}")
    private long grpTtl;

    @Bean
    public CacheManager cacheManager(
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            RedisConnectionFactory factory) {
        if (!codeEnabled || factory == null) {
            return new ConcurrentMapCacheManager("detail", "grp");
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

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
