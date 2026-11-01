package com.example.prop.service;

import com.example.prop.property.DbProperty;

import java.util.List;

public interface PropertyService {
    List<DbProperty> findAll();
    List<DbProperty> findAll(DbProperty filter);
    void save(DbProperty property);
    void delete(DbProperty property);
    int reload();
    List<String> getActiveProfiles();
    List<String> getCategories();
}
