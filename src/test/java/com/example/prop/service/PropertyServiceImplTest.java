package com.example.prop.service;

import com.example.prop.property.DbProperty;
import com.example.prop.property.PropertyMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PropertyServiceImpl 통합 테스트.
 * @Transactional 로 각 테스트 후 롤백 — DB 오염 없음.
 * afterCommit() 콜백은 롤백 시 실행되지 않으므로 reload 검증은 제외.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PropertyServiceImplTest {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyMapper propertyMapper;

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    @Test
    void findAll_returnsSeededRows() {
        assertThat(propertyService.findAll()).isNotEmpty();
    }

    // -----------------------------------------------------------------------
    // save (upsert)
    // -----------------------------------------------------------------------

    @Test
    void save_insertsNewProperty() {
        DbProperty p = prop("svc-site", "default", "new.key", "new-val");

        propertyService.save(p);

        List<DbProperty> found = propertyMapper.findAll(siteFilter("svc-site"));
        assertThat(found)
            .extracting(DbProperty::getPropertyKey)
            .contains("new.key");
        assertThat(found)
            .filteredOn(d -> "new.key".equals(d.getPropertyKey()))
            .extracting(DbProperty::getPropertyValue)
            .containsExactly("new-val");
    }

    @Test
    void save_updatesExistingProperty() {
        DbProperty p = prop("svc-site", "default", "upd.key", "original");
        propertyMapper.insert(p);

        p.setPropertyValue("updated");
        propertyService.save(p);  // upsert: update 분기

        assertThat(propertyMapper.findAll(siteFilter("svc-site")))
            .filteredOn(d -> "upd.key".equals(d.getPropertyKey()))
            .extracting(DbProperty::getPropertyValue)
            .containsExactly("updated");
    }

    @Test
    void save_twiceWithSameKey_nosDuplicate() {
        DbProperty p = prop("svc-site", "default", "dup.key", "v1");
        propertyService.save(p);

        p.setPropertyValue("v2");
        propertyService.save(p);

        assertThat(propertyMapper.findAll(siteFilter("svc-site")))
            .filteredOn(d -> "dup.key".equals(d.getPropertyKey()))
            .hasSize(1)
            .extracting(DbProperty::getPropertyValue)
            .containsExactly("v2");
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    void delete_removesRow() {
        DbProperty p = prop("svc-site", "default", "del.key", "bye");
        propertyMapper.insert(p);

        propertyService.delete(p);

        assertThat(propertyMapper.findAll(siteFilter("svc-site"))).isEmpty();
    }

    @Test
    void delete_nonExistentKey_doesNotThrow() {
        DbProperty p = prop("svc-site", "default", "ghost.key", null);
        propertyService.delete(p);  // 행 없어도 예외 없어야 함
    }

    // -----------------------------------------------------------------------
    // findAll (filter)
    // -----------------------------------------------------------------------

    @Test
    void findAll_withSeFilter_returnsFilteredRows() {
        DbProperty p = prop("svc-site", "default", "se.filter.key", "val");
        p.setSe("APP");
        propertyMapper.insert(p);

        DbProperty filter = new DbProperty();
        filter.setSite("svc-site");
        filter.setSe("APP");
        List<DbProperty> result = propertyService.findAll(filter);
        assertThat(result).extracting(DbProperty::getSe).containsOnly("APP");
    }

    // -----------------------------------------------------------------------
    // reload
    // -----------------------------------------------------------------------

    @Test
    void reload_returnsNonNegativeKeyCount() {
        int count = propertyService.reload();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // getCategories
    // -----------------------------------------------------------------------

    @Test
    void getCategories_returnsListWithoutException() {
        List<String> categories = propertyService.getCategories();
        assertThat(categories).isNotNull();
    }

    // -----------------------------------------------------------------------
    // getActiveProfiles
    // -----------------------------------------------------------------------

    @Test
    void getActiveProfiles_alwaysContainsDefault() {
        assertThat(propertyService.getActiveProfiles()).contains("default");
    }

    @Test
    void getActiveProfiles_containsTestProfile() {
        // @ActiveProfiles("test") 이므로 "test"가 포함되어야 함
        assertThat(propertyService.getActiveProfiles()).contains("test");
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private DbProperty prop(String site, String profile, String key, String value) {
        DbProperty p = new DbProperty();
        p.setSite(site);
        p.setProfile(profile);
        p.setPropertyKey(key);
        p.setPropertyValue(value);
        return p;
    }

    private DbProperty siteFilter(String site) {
        DbProperty cond = new DbProperty();
        cond.setSite(site);
        return cond;
    }
}
