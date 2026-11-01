package com.example.prop.property;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 활성 프로파일·사이트 목록 해석 유틸.
 * DatabasePropertyLoader와 PropertyServiceImpl에서 공유한다.
 */
public final class ProfileUtils {

    static final String DEFAULT = "default";

    private ProfileUtils() {}

    /**
     * 로딩 순서: "default" 먼저, 이후 활성(또는 기본) 프로파일 추가.
     * LinkedHashSet으로 중복 제거 — 프로파일 미설정 시 "default"가 두 번 들어가던 버그 수정.
     */
    public static List<String> resolveProfiles(ConfigurableEnvironment env) {
        Set<String> set = new LinkedHashSet<>();
        set.add(DEFAULT);
        String[] profiles = env.getActiveProfiles();
        Collections.addAll(set, ObjectUtils.isEmpty(profiles) ? env.getDefaultProfiles() : profiles);
        return new ArrayList<>(set);
    }

    public static List<String> resolveSites(ConfigurableEnvironment env) {
        Set<String> set = new LinkedHashSet<>();
        set.add(DEFAULT);
        String site = env.getProperty("grip.site");
        if (site != null && !site.isEmpty()) set.add(site);
        return new ArrayList<>(set);
    }
}
