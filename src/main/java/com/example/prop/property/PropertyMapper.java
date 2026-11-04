package com.example.prop.property;

import java.util.List;

public interface PropertyMapper {

    List<DbProperty> findAll(DbProperty condition);

    default List<DbProperty> findAll() {
        return findAll(null);
    }

    List<String> findDistinctSe();

    int update(DbProperty property);

    int insert(DbProperty property);

    int delete(DbProperty property);
}
