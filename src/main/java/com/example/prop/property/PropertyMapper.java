package com.example.prop.property;

import org.apache.ibatis.annotations.*;

import java.util.List;

public interface PropertyMapper {

    @SelectProvider(type = PropertySqlProvider.class, method = "findAll")
    List<DbProperty> findAll(DbProperty condition);

    @SelectProvider(type = PropertySqlProvider.class, method = "findDistinctSe")
    List<String> findDistinctSe();

    @UpdateProvider(type = PropertySqlProvider.class, method = "update")
    int update(DbProperty property);

    @InsertProvider(type = PropertySqlProvider.class, method = "insert")
    int insert(DbProperty property);

    @DeleteProvider(type = PropertySqlProvider.class, method = "delete")
    int delete(DbProperty property);
}
