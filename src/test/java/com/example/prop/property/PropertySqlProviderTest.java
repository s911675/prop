package com.example.prop.property;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertySqlProviderTest {

    private final PropertySqlProvider provider = new PropertySqlProvider();

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    @Test
    void findAll_nullCondition_noWhereClause() {
        String sql = provider.findAll(null);
        assertThat(sql).doesNotContainIgnoringCase("WHERE");
        assertThat(sql).contains("ORDER BY property_key");
    }

    @Test
    void findAll_emptyCondition_noWhereClause() {
        String sql = provider.findAll(new DbProperty());
        assertThat(sql).doesNotContainIgnoringCase("WHERE");
    }

    @Test
    void findAll_siteOnly_filtersBySite() {
        DbProperty cond = new DbProperty();
        cond.setSite("myapp");
        String sql = provider.findAll(cond);
        assertThat(sql).contains("site = #{site}");
        assertThat(sql).doesNotContain("profile = #{profile}");
    }

    @Test
    void findAll_profileOnly_filtersByProfile() {
        DbProperty cond = new DbProperty();
        cond.setProfile("dev");
        String sql = provider.findAll(cond);
        assertThat(sql).contains("profile = #{profile}");
        assertThat(sql).doesNotContain("site = #{site}");
    }

    @Test
    void findAll_siteAndProfile_filtersByBoth() {
        DbProperty cond = new DbProperty();
        cond.setSite("myapp");
        cond.setProfile("dev");
        String sql = provider.findAll(cond);
        assertThat(sql).contains("site = #{site}", "profile = #{profile}");
    }

    @Test
    void findAll_seFilter_containsSeWhere() {
        DbProperty cond = new DbProperty();
        cond.setSe("APP");
        String sql = provider.findAll(cond);
        assertThat(sql).contains("se = #{se}");
        assertThat(sql).doesNotContain("site = #{site}");
    }

    @Test
    void findDistinctSe_selectDistinct() {
        String sql = provider.findDistinctSe();
        assertThat(sql).containsIgnoringCase("SELECT DISTINCT");
        assertThat(sql).containsIgnoringCase("se");
        assertThat(sql).containsIgnoringCase("zt_property_source");
    }

    // -----------------------------------------------------------------------
    // insert
    // -----------------------------------------------------------------------

    @Test
    void insert_containsAllValuePlaceholders() {
        String sql = provider.insert(new DbProperty());
        assertThat(sql).containsIgnoringCase("INSERT INTO zt_property_source");
        assertThat(sql).contains("#{site}", "#{profile}", "#{propertyKey}", "#{propertyValue}");
    }

    // -----------------------------------------------------------------------
    // update
    // -----------------------------------------------------------------------

    @Test
    void update_setsValueColumns_andFiltersOnPk() {
        String sql = provider.update(new DbProperty());
        assertThat(sql).containsIgnoringCase("UPDATE zt_property_source");
        assertThat(sql).contains("property_value = #{propertyValue}");
        assertThat(sql).contains("site = #{site}", "profile = #{profile}", "property_key = #{propertyKey}");
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    void delete_filtersOnPk() {
        String sql = provider.delete(new DbProperty());
        assertThat(sql).containsIgnoringCase("DELETE FROM zt_property_source");
        assertThat(sql).contains("site = #{site}", "profile = #{profile}", "property_key = #{propertyKey}");
    }
}
