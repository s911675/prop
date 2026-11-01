package com.example.prop.config;

import com.example.prop.property.DatabasePropertySourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropertiesConfig {

    /**
     * static 메서드로 등록해야 BeanFactoryPostProcessor가 조기에 인스턴스화된다.
     * ignoreUnresolvablePlaceholders=true: DB에 없는 키는 다음 PropertySource에서 해결.
     */
    @Bean
    public static DatabasePropertySourceConfigurer databasePropertySourceConfigurer() {
        DatabasePropertySourceConfigurer configurer = new DatabasePropertySourceConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }
}
