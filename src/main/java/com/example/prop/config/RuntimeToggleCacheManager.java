package com.example.prop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.env.Environment;

import java.util.Collection;

/**
 * CacheManager that decides at every getCache() call whether to use Valkey or
 * the in-process ConcurrentMap, based on runtime-readable environment properties:
 *
 *   valkey.enabled=false     → all caches use JVM-local ConcurrentMap
 *   cache.code.enabled=false → same
 *   (both default to true)
 *
 * When Valkey is chosen, each cache is wrapped in FallbackCache so connection
 * failures and deserialization errors are absorbed transparently.
 *
 * These properties are stored in zt_property_source and reload without restart.
 */
@Slf4j
public class RuntimeToggleCacheManager implements CacheManager {

    private final CacheManager  redisManager;
    private final ConcurrentMapCacheManager localManager;
    private final Environment   env;

    public RuntimeToggleCacheManager(CacheManager redisManager,
                                     ConcurrentMapCacheManager localManager,
                                     Environment env) {
        this.redisManager  = redisManager;
        this.localManager  = localManager;
        this.env           = env;
    }

    @Override
    public Cache getCache(String name) {
        if (!isValkeyEnabled()) {
            return localManager.getCache(name);
        }
        Cache redis = redisManager.getCache(name);
        if (redis == null) {
            return localManager.getCache(name);
        }
        return new FallbackCache(redis, name, env);
    }

    @Override
    public Collection<String> getCacheNames() {
        return redisManager.getCacheNames();
    }

    private boolean isValkeyEnabled() {
        boolean valkeyOn = "true".equalsIgnoreCase(env.getProperty("valkey.enabled",     "true"));
        boolean codeOn   = "true".equalsIgnoreCase(env.getProperty("cache.code.enabled", "true"));
        return valkeyOn && codeOn;
    }
}
