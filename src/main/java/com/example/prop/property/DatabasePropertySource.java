package com.example.prop.property;

import org.springframework.core.env.PropertySource;

import java.util.Map;

/**
 * DB에서 읽은 Map을 Spring PropertySource로 감싸는 어댑터.
 * MutablePropertySources.addFirst()로 등록하여 파일 기반 설정보다 높은 우선순위를 가진다.
 */
public class DatabasePropertySource extends PropertySource<Map<String, Object>> {

    public DatabasePropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }
}
