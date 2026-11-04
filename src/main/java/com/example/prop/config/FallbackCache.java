package com.example.prop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.core.env.Environment;

import java.util.concurrent.Callable;

/**
 * Wraps a Redis Cache so that any connection/deserialization error is absorbed:
 *   get  → returns null  (triggers @Cacheable to fetch from DB)
 *   put  → logs and skips (next read will re-fetch from DB)
 *   evict/clear → logs and skips
 *
 * Also respects runtime flags (checked on every call via Environment):
 *   valkey.enabled=false     → behaves as a no-op cache (always miss)
 *   cache.code.enabled=false → same
 */
@Slf4j
public class FallbackCache implements Cache {

    private final Cache delegate;
    private final String name;
    private final Environment env;

    public FallbackCache(Cache delegate, String name, Environment env) {
        this.delegate = delegate;
        this.name     = name;
        this.env      = env;
    }

    @Override public String getName()        { return name; }
    @Override public Object getNativeCache() { return delegate.getNativeCache(); }

    @Override
    public ValueWrapper get(Object key) {
        if (!isEnabled()) return null;
        try {
            return delegate.get(key);
        } catch (Exception e) {
            log.warn("Valkey get failed [{}::{}]: {}", name, key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        if (!isEnabled()) return null;
        try {
            return delegate.get(key, type);
        } catch (Exception e) {
            log.warn("Valkey get failed [{}::{}]: {}", name, key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (!isEnabled()) {
            return callLoader(key, valueLoader);
        }
        try {
            return delegate.get(key, valueLoader);
        } catch (Exception e) {
            log.warn("Valkey get failed [{}::{}]: {}", name, key, e.getMessage());
            return callLoader(key, valueLoader);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (!isEnabled()) return;
        try {
            delegate.put(key, value);
        } catch (Exception e) {
            log.warn("Valkey put failed [{}::{}]: {}", name, key, e.getMessage());
        }
    }

    @Override
    public void evict(Object key) {
        if (!isEnabled()) return;
        try {
            delegate.evict(key);
        } catch (Exception e) {
            log.warn("Valkey evict failed [{}::{}]: {}", name, key, e.getMessage());
        }
    }

    @Override
    public void clear() {
        if (!isEnabled()) return;
        try {
            delegate.clear();
        } catch (Exception e) {
            log.warn("Valkey clear failed [{}]: {}", name, e.getMessage());
        }
    }

    private boolean isEnabled() {
        return "true".equalsIgnoreCase(env.getProperty("valkey.enabled",      "true"))
            && "true".equalsIgnoreCase(env.getProperty("cache.code.enabled",  "true"));
    }

    private <T> T callLoader(Object key, Callable<T> loader) {
        try {
            return loader.call();
        } catch (Exception e) {
            throw new ValueRetrievalException(key, loader, e);
        }
    }
}
