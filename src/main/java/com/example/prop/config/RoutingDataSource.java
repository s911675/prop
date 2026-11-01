package com.example.prop.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 readOnly 여부에 따라 DataSource를 분기한다.
 *
 * readOnly=true  (@Transactional(readOnly=true)) → "replica"
 * readOnly=false (@Transactional)                → "primary"
 *
 * LazyConnectionDataSourceProxy와 함께 사용해야 한다.
 * 없으면 커넥션 획득이 readOnly 플래그 설정 전에 일어나 항상 primary로 라우팅된다.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    static final String PRIMARY = "primary";
    static final String REPLICA  = "replica";

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
               ? REPLICA : PRIMARY;
    }
}
