package com.example.prop.controller;

import com.example.prop.property.DatabasePropertySourceConfigurer;
import com.example.prop.property.DbProperty;
import com.example.prop.property.PropertyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PropertyController MockMvc 통합 테스트.
 *
 * @Transactional 테스트: HTTP 응답 계약 검증 (저장·삭제 후 롤백).
 * saveAndResolve_e2e: @Transactional 없음 — 실제 커밋 후 afterCommit reload 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PropertyMapper propertyMapper;
    @Autowired private DatabasePropertySourceConfigurer configurer;

    // -----------------------------------------------------------------------
    // GET /api/properties
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void findAll_returns200WithNonEmptyArray() throws Exception {
        mockMvc.perform(get("/api/properties"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }

    // -----------------------------------------------------------------------
    // POST /api/properties
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void save_returns200WithOkStatus() throws Exception {
        mockMvc.perform(post("/api/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("ctrl-test", "default", "ctrl.save.key", "ctrl-val")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").exists());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/properties
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void delete_afterSave_returns200WithOkStatus() throws Exception {
        String body = json("ctrl-test", "default", "ctrl.del.key", "del-val");

        mockMvc.perform(post("/api/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    // -----------------------------------------------------------------------
    // POST /api/properties/reload
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void reload_returns200WithOkStatus() throws Exception {
        mockMvc.perform(post("/api/properties/reload"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    // -----------------------------------------------------------------------
    // GET /api/properties/resolve/{key}
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void resolve_existingKey_returnsDbValue() throws Exception {
        // data.sql 시드: myapp/default app.title = "Prop Demo App"
        mockMvc.perform(get("/api/properties/resolve/app.title"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("app.title"))
            .andExpect(jsonPath("$.value").value("Prop Demo App"))
            .andExpect(jsonPath("$.activeProfiles").isArray());
    }

    @Test
    @Transactional
    void resolve_dbOverridesFileProperty() throws Exception {
        // application.properties: app.version=0.0.1  /  data.sql: app.version=1.0.0-DB
        mockMvc.perform(get("/api/properties/resolve/app.version"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("1.0.0-DB"));
    }

    @Test
    @Transactional
    void resolve_missingKey_returnsNotFoundString() throws Exception {
        mockMvc.perform(get("/api/properties/resolve/no.such.key.xyz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("(not found)"));
    }

    // -----------------------------------------------------------------------
    // POST /api/properties — 유효성 검증 (400)
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void save_blankSite_returns400() throws Exception {
        DbProperty p = new DbProperty();
        p.setSite("");            // @NotBlank 위반
        p.setProfile("default");
        p.setPropertyKey("some.key");
        mockMvc.perform(post("/api/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.site").exists());
    }

    @Test
    @Transactional
    void save_blankKey_returns400() throws Exception {
        DbProperty p = new DbProperty();
        p.setSite("myapp");
        p.setProfile("default");
        p.setPropertyKey("");     // @NotBlank 위반
        mockMvc.perform(post("/api/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.propertyKey").exists());
    }

    // -----------------------------------------------------------------------
    // GET /api/properties?site=...
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void findAll_withSiteFilter_returnsOnlySiteRows() throws Exception {
        mockMvc.perform(get("/api/properties").param("site", "myapp"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].site").value(org.hamcrest.Matchers.everyItem(
                org.hamcrest.Matchers.is("myapp"))));
    }

    // -----------------------------------------------------------------------
    // GET /api/properties/categories
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void categories_returns200WithArray() throws Exception {
        mockMvc.perform(get("/api/properties/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // -----------------------------------------------------------------------
    // GET /api/properties/context
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    void context_returnsCurrentState() throws Exception {
        mockMvc.perform(get("/api/properties/context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.site").exists())
            .andExpect(jsonPath("$.activeProfiles").isArray())
            .andExpect(jsonPath("$.keysLoaded").isNumber())
            .andExpect(jsonPath("$.sourceName").value("DatabasePropertySource"));
    }

    // -----------------------------------------------------------------------
    // E2E: 저장 → afterCommit reload → resolve 에서 즉시 반영 확인
    // @Transactional 없음: 실제 커밋 + afterCommit 콜백 검증
    // -----------------------------------------------------------------------

    @Test
    void saveAndResolve_e2e_afterCommitReflectsInEnvironment() throws Exception {
        String key = "e2e.test.key";
        String value = "e2e-value";

        try {
            mockMvc.perform(post("/api/properties")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json("myapp", "default", key, value)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

            // afterCommit이 실행돼 PropertySource가 교체된 상태여야 함
            mockMvc.perform(get("/api/properties/resolve/" + key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(value));

        } finally {
            // 정리: DB에서 삭제 후 reload (다른 테스트 격리)
            propertyMapper.delete(prop("myapp", "default", key, null));
            configurer.reload();
        }
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private String json(String site, String profile, String key, String value) throws Exception {
        return objectMapper.writeValueAsString(prop(site, profile, key, value));
    }

    private DbProperty prop(String site, String profile, String key, String value) {
        DbProperty p = new DbProperty();
        p.setSite(site);
        p.setProfile(profile);
        p.setPropertyKey(key);
        p.setPropertyValue(value);
        return p;
    }
}
