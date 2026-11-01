package com.example.prop.property;

import org.apache.ibatis.jdbc.SQL;

public class PropertySqlProvider {

    private static final String TABLE = "zt_property_source";
    private static final String COLUMNS = "site, profile, property_key, se, property_name, property_value";

    public String findAll(DbProperty condition) {
        SQL sql = new SQL();
        sql.SELECT(COLUMNS);
        sql.FROM(TABLE);
        if (condition != null) {
            if (condition.getSite() != null)    sql.WHERE("site = #{site}");
            if (condition.getProfile() != null) sql.WHERE("profile = #{profile}");
            if (condition.getSe() != null)      sql.WHERE("se = #{se}");
        }
        sql.ORDER_BY("property_key");
        return sql.toString();
    }

    public String findDistinctSe() {
        SQL sql = new SQL();
        sql.SELECT_DISTINCT("se");
        sql.FROM(TABLE);
        sql.WHERE("se IS NOT NULL");
        return sql.toString();
    }

    public String update(DbProperty p) {
        SQL sql = new SQL();
        sql.UPDATE(TABLE);
        sql.SET("se = #{se}");
        sql.SET("property_name = #{propertyName}");
        sql.SET("property_value = #{propertyValue}");
        sql.WHERE("site = #{site}");
        sql.WHERE("profile = #{profile}");
        sql.WHERE("property_key = #{propertyKey}");
        return sql.toString();
    }

    public String insert(DbProperty p) {
        SQL sql = new SQL();
        sql.INSERT_INTO(TABLE);
        sql.VALUES("site",         "#{site}");
        sql.VALUES("profile",      "#{profile}");
        sql.VALUES("property_key", "#{propertyKey}");
        sql.VALUES("se",           "#{se}");
        sql.VALUES("property_name",  "#{propertyName}");
        sql.VALUES("property_value", "#{propertyValue}");
        return sql.toString();
    }

    public String delete(DbProperty p) {
        SQL sql = new SQL();
        sql.DELETE_FROM(TABLE);
        sql.WHERE("site = #{site}");
        sql.WHERE("profile = #{profile}");
        sql.WHERE("property_key = #{propertyKey}");
        return sql.toString();
    }
}
