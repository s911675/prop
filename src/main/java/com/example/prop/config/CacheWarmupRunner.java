package com.example.prop.config;

import com.example.prop.code.ComCdService;
import com.example.prop.code.ComGrpCd;
import com.example.prop.code.ComGrpCdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Warms up the Valkey code cache on startup.
 *
 * Run order:
 *   @Order(1) PropertyAutoReloadRunner  — loads DB properties (incl. feature flags)
 *   @Order(2) CacheWarmupRunner         — this runner, checks flags then warms up
 *
 * Multi-instance strategy:
 *   Before warming up, check whether code:grp:all already exists in Valkey.
 *   If present, another instance already warmed up → skip.
 *   If absent, warm up by calling existing @Cacheable service methods.
 *   Edge case: simultaneous startup may cause N instances to all warm up once,
 *   but writing the same data is harmless.
 *
 *   POST /api/cache/warmup forces a re-warmup without the already-warm check.
 */
@Slf4j
@Order(2)
@Component
public class CacheWarmupRunner implements ApplicationRunner {

    @Autowired private ComGrpCdService comGrpCdService;
    @Autowired private ComCdService    comCdService;
    @Autowired private CacheManager    cacheManager;
    @Autowired private Environment     env;

    @Override
    public void run(ApplicationArguments args) {
        if (!isEnabled()) {
            log.info("Cache warmup skipped (cache.warmup.enabled, valkey.enabled, or cache.code.enabled is false)");
            return;
        }

        // Skip if another instance already warmed up Valkey
        Cache grpCache = cacheManager.getCache("grp");
        if (grpCache != null && grpCache.get("all") != null) {
            log.info("Cache already warm (code:grp:all exists), skipping startup warmup");
            return;
        }

        doWarmup();
    }

    /**
     * Warms up the cache by calling @Cacheable service methods.
     * Called by run() on startup and by CacheController on demand.
     * Does NOT check whether the cache is already warm — always executes.
     */
    public WarmupResult doWarmup() {
        log.info("Cache warmup started");
        List<ComGrpCd> groups = comGrpCdService.findAll();  // → code:grp:all
        int detailCount = 0;
        for (ComGrpCd g : groups) {
            comGrpCdService.findById(g.getGrpCd());                           // → code:grp:{grpCd}
            detailCount += comCdService.findByGrpCd(g.getGrpCd()).size();     // → code:detail:{grpCd}
        }
        log.info("Cache warmup done: {} groups, {} detail entries", groups.size(), detailCount);
        return new WarmupResult(groups.size(), detailCount);
    }

    private boolean isEnabled() {
        return "true".equalsIgnoreCase(env.getProperty("cache.warmup.enabled", "true"))
            && "true".equalsIgnoreCase(env.getProperty("valkey.enabled",       "true"))
            && "true".equalsIgnoreCase(env.getProperty("cache.code.enabled",   "true"));
    }

    public record WarmupResult(int grpCount, int detailCount) {}
}
