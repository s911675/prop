package com.example.prop.config;

import com.example.prop.property.DatabasePropertySourceConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 앱 기동 완료 직후 DB 프라퍼티를 재로딩한다.
 *
 * 이유: DatabasePropertySourceConfigurer.setEnvironment()는 Spring 초기화 전에 실행되어
 * schema.sql / data.sql이 아직 적용되지 않은 시점에 DB를 조회한다.
 * ApplicationRunner는 ApplicationContext 완전 초기화 후 실행되므로
 * 이 시점에 reload하면 data.sql로 삽입된 데이터가 정상 로딩된다.
 */
@Slf4j
@Order(1)
@Component
public class PropertyAutoReloadRunner implements ApplicationRunner {

    @Autowired
    private DatabasePropertySourceConfigurer configurer;

    @Override
    public void run(ApplicationArguments args) {
        log.info("ApplicationRunner: reloading DB property source after full context initialization");
        int keysLoaded = configurer.reload();
        log.info("ApplicationRunner: reload complete — {} keys loaded", keysLoaded);
    }
}
