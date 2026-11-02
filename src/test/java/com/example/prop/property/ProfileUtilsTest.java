package com.example.prop.property;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProfileUtils.resolveProfiles / resolveSites 단위 테스트.
 * MockEnvironment: 시스템 프라퍼티·환경변수를 포함하지 않는 깨끗한 환경.
 */
class ProfileUtilsTest {

    // -----------------------------------------------------------------------
    // resolveProfiles
    // -----------------------------------------------------------------------

    @Test
    void resolveProfiles_noActiveProfile_returnsOnlyDefault() {
        // 아무 설정 없음 → Spring defaultProfiles = ["default"]
        MockEnvironment env = new MockEnvironment();

        List<String> profiles = ProfileUtils.resolveProfiles(env);

        assertThat(profiles).containsExactly("default");
    }

    @Test
    void resolveProfiles_activeProfileDefault_noDuplicate() {
        // spring.profiles.active=default 로 명시 설정한 경우
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("default");

        List<String> profiles = ProfileUtils.resolveProfiles(env);

        // LinkedHashSet이 중복 제거 → ["default"] (두 번 들어가면 안 됨)
        assertThat(profiles).containsExactly("default");
        assertThat(profiles).hasSize(1);
    }

    @Test
    void resolveProfiles_activeDev_returnsDefaultThenDev() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        List<String> profiles = ProfileUtils.resolveProfiles(env);

        assertThat(profiles).containsExactly("default", "dev");
    }

    @Test
    void resolveProfiles_activeDevAndProd_returnsDefaultThenDevThenProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev", "prod");

        List<String> profiles = ProfileUtils.resolveProfiles(env);

        assertThat(profiles).containsExactly("default", "dev", "prod");
    }

    @Test
    void resolveProfiles_activeDefaultAndDev_noDuplicate() {
        // spring.profiles.active=default,dev 처럼 default를 함께 명시한 경우
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("default", "dev");

        List<String> profiles = ProfileUtils.resolveProfiles(env);

        // "default"는 맨 앞 1회만
        assertThat(profiles).containsExactly("default", "dev");
        assertThat(profiles).hasSize(2);
    }

    @Test
    void resolveProfiles_orderIsPreserved() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        List<String> profiles = ProfileUtils.resolveProfiles(env);

        // "default"가 항상 첫 번째 (공통 기본값이 먼저 로드돼야 함)
        assertThat(profiles.get(0)).isEqualTo("default");
    }

    // -----------------------------------------------------------------------
    // resolveSites
    // -----------------------------------------------------------------------

    @Test
    void resolveSites_noGripSite_returnsOnlyDefault() {
        // grip.site 없음
        MockEnvironment env = new MockEnvironment();

        List<String> sites = ProfileUtils.resolveSites(env);

        assertThat(sites).containsExactly("default");
    }

    @Test
    void resolveSites_withGripSite_returnsDefaultThenSite() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("grip.site", "myapp");

        List<String> sites = ProfileUtils.resolveSites(env);

        assertThat(sites).containsExactly("default", "myapp");
    }

    @Test
    void resolveSites_gripSiteEqualsDefault_noDuplicate() {
        // grip.site=default 로 잘못 설정한 극단적 케이스
        MockEnvironment env = new MockEnvironment();
        env.setProperty("grip.site", "default");

        List<String> sites = ProfileUtils.resolveSites(env);

        assertThat(sites).containsExactly("default");
        assertThat(sites).hasSize(1);
    }
}
