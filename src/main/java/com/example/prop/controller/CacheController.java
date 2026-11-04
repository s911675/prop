package com.example.prop.controller;

import com.example.prop.config.CacheWarmupRunner;
import com.example.prop.dto.WarmupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private CacheWarmupRunner warmupRunner;

    /**
     * On-demand cache warmup.
     * Unlike the startup runner, this always executes regardless of whether
     * the cache is already warm — use after Valkey recovery or manual flush.
     */
    @PostMapping("/warmup")
    public WarmupResponse warmup() {
        CacheWarmupRunner.WarmupResult r = warmupRunner.doWarmup();
        return new WarmupResponse("ok", "warmup complete", r.grpCount(), r.detailCount());
    }
}
