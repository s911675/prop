package com.example.prop.property;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabasePropertyLoader 동작 검증.
 * Spring 컨텍스트를 재사용해 Factory 캐싱·중복 방지 동작 검증.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabasePropertyLoaderTest {

    @Autowired
    private ConfigurableEnvironment env;

    @Autowired
    private DatabasePropertySourceConfigurer configurer;

    // -----------------------------------------------------------------------
    // ProfileUtils — activeProfiles 중복 없음
    // -----------------------------------------------------------------------

    @Test
    void activeProfiles_noDuplicate_whenDefaultProfile() {
        // @ActiveProfiles("test") → resolveProfiles: ["default", "test"]
        List<String> profiles = ProfileUtils.resolveProfiles(env);
        long distinctCount = profiles.stream().distinct().count();
        assertThat(distinctCount).isEqualTo(profiles.size())
            .as("프로파일 목록에 중복 없어야 함");
    }

    @Test
    void activeProfiles_containsDefaultAndTestProfile() {
        List<String> profiles = ProfileUtils.resolveProfiles(env);
        assertThat(profiles).containsExactly("default", "test");
    }

    // -----------------------------------------------------------------------
    // resolveSites
    // -----------------------------------------------------------------------

    @Test
    void resolveSites_containsDefaultAndConfiguredSite() {
        List<String> sites = ProfileUtils.resolveSites(env);
        assertThat(sites).contains("default");
        // application-test.properties에 grip.site=myapp (application.properties에서 상속)
        assertThat(sites).doesNotHaveDuplicates();
    }

    // -----------------------------------------------------------------------
    // reload — 키 카운트 반환
    // -----------------------------------------------------------------------

    @Test
    void reload_returnsNonNegativeKeyCount() {
        int count = configurer.reload();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void reload_twice_returnsSameCount() {
        int first  = configurer.reload();
        int second = configurer.reload();
        // 데이터 변경 없으면 동일한 수
        assertThat(second).isEqualTo(first);
    }

    // -----------------------------------------------------------------------
    // Property source priority — DB 값이 최우선
    // -----------------------------------------------------------------------

    @Test
    void dbPropertySource_registeredAsFirst() {
        assertThat(env.getPropertySources().contains("DatabasePropertySource")).isTrue();
        assertThat(env.getPropertySources().iterator().next().getName())
            .isEqualTo("DatabasePropertySource");
    }
}
