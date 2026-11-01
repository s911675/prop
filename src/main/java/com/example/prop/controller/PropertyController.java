package com.example.prop.controller;

import com.example.prop.dto.ApiResponse;
import com.example.prop.dto.ContextResponse;
import com.example.prop.dto.ReloadResponse;
import com.example.prop.dto.ResolveResponse;
import com.example.prop.property.DbProperty;
import com.example.prop.property.ProfileUtils;
import com.example.prop.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@Validated
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ConfigurableEnvironment env;

    /** 전체 또는 필터 조회 */
    @GetMapping
    public List<DbProperty> findAll(
            @RequestParam(required = false) String site,
            @RequestParam(required = false) String profile,
            @RequestParam(required = false) String se) {
        DbProperty filter = new DbProperty();
        filter.setSite(site);
        filter.setProfile(profile);
        filter.setSe(se);
        return propertyService.findAll(filter);
    }

    /** 저장(upsert) + 자동 reload */
    @PostMapping
    public ApiResponse save(@Valid @RequestBody DbProperty property) {
        propertyService.save(property);
        return new ApiResponse("ok", "저장 및 reload 완료");
    }

    /** 삭제 + 자동 reload */
    @DeleteMapping
    public ApiResponse delete(
            @RequestParam @NotBlank String site,
            @RequestParam @NotBlank String profile,
            @RequestParam @NotBlank String propertyKey) {
        DbProperty property = new DbProperty();
        property.setSite(site);
        property.setProfile(profile);
        property.setPropertyKey(propertyKey);
        propertyService.delete(property);
        return new ApiResponse("ok", "삭제 및 reload 완료");
    }

    /** DB 강제 reload */
    @PostMapping("/reload")
    public ReloadResponse reload() {
        int keysLoaded = propertyService.reload();
        return new ReloadResponse("ok", "reload 완료", keysLoaded);
    }

    /** 키의 우선순위 적용 최종값 조회 */
    @GetMapping("/resolve/{key}")
    public ResolveResponse resolve(@PathVariable String key) {
        String value = env.getProperty(key);
        return new ResolveResponse(key, value != null ? value : "(not found)", propertyService.getActiveProfiles());
    }

    /** se(카테고리) distinct 목록 */
    @GetMapping("/categories")
    public List<String> categories() {
        return propertyService.getCategories();
    }

    /** 현재 활성 site/profile 컨텍스트 및 로드된 키 수 */
    @GetMapping("/context")
    public ContextResponse context() {
        List<String> profiles = ProfileUtils.resolveProfiles(env);
        PropertySource<?> source = env.getPropertySources().get("DatabasePropertySource");
        int keysLoaded = (source instanceof EnumerablePropertySource)
                ? ((EnumerablePropertySource<?>) source).getPropertyNames().length : 0;
        return new ContextResponse(
                env.getProperty("grip.site", "default"),
                profiles,
                keysLoaded,
                "DatabasePropertySource");
    }
}
