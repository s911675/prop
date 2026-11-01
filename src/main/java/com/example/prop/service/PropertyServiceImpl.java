package com.example.prop.service;

import com.example.prop.property.DatabasePropertySourceConfigurer;
import com.example.prop.property.DbProperty;
import com.example.prop.property.ProfileUtils;
import com.example.prop.property.PropertyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
public class PropertyServiceImpl implements PropertyService {

    @Autowired
    private PropertyMapper propertyMapper;

    @Autowired
    private DatabasePropertySourceConfigurer configurer;

    @Autowired
    private ConfigurableEnvironment env;

    @Transactional(readOnly = true)
    @Override
    public List<DbProperty> findAll() {
        return propertyMapper.findAll(new DbProperty());
    }

    @Transactional(readOnly = true)
    @Override
    public List<DbProperty> findAll(DbProperty filter) {
        return propertyMapper.findAll(filter);
    }

    /**
     * 저장(upsert): update → 0행이면 insert.
     * 트랜잭션 커밋 성공 후에만 reload → 롤백 시 오염 방지.
     */
    @Transactional
    @Override
    public void save(DbProperty property) {
        int rows = propertyMapper.update(property);
        if (rows == 0) {
            propertyMapper.insert(property);
        }
        reloadAfterCommit();
    }

    @Transactional
    @Override
    public void delete(DbProperty property) {
        propertyMapper.delete(property);
        reloadAfterCommit();
    }

    @Override
    public int reload() {
        return configurer.reload();
    }

    @Override
    public List<String> getActiveProfiles() {
        return ProfileUtils.resolveProfiles(env);
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> getCategories() {
        return propertyMapper.findDistinctSe();
    }

    private void reloadAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    configurer.reload();
                } catch (Exception e) {
                    // afterCommit 내 예외는 TransactionSystemException으로 감싸져 500을 유발하므로 흡수
                    log.error("Property reload failed after commit — next reload will sync", e);
                }
            }
        });
    }
}
