package com.example.prop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Primary / Replica DataSource 구성.
 *
 * primary  → 쓰기 전용 (@Transactional)
 * replica  → 읽기 전용 (@Transactional(readOnly = true))
 *
 * LazyConnectionDataSourceProxy: 커넥션 획득 시점을 트랜잭션 바인딩 이후로 지연해
 * readOnly 플래그가 세팅된 뒤 RoutingDataSource.determineCurrentLookupKey()가 호출되도록 보장.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "db.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "db.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSource primaryDataSource, DataSource replicaDataSource) {
        Map<Object, Object> targets = new HashMap<>();
        targets.put(RoutingDataSource.PRIMARY, primaryDataSource);
        targets.put(RoutingDataSource.REPLICA,  replicaDataSource);

        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(primaryDataSource);
        routing.afterPropertiesSet();

        return new LazyConnectionDataSourceProxy(routing);
    }
}
