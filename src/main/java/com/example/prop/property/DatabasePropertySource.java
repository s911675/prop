package com.example.prop.property;

import org.springframework.core.env.EnumerablePropertySource;

import java.util.Map;

/**
 * DB에서 읽은 Map을 Spring PropertySource로 감싸는 어댑터.
 * EnumerablePropertySource를 상속해 getPropertyNames()를 제공하므로
 * /context 엔드포인트에서 실제 로드된 키 수를 정확히 조회할 수 있다.
 */
public class DatabasePropertySource extends EnumerablePropertySource<Map<String, Object>> {

    public DatabasePropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public String[] getPropertyNames() {
        return source.keySet().toArray(new String[0]);
    }

    @Override
    public Object getProperty(String name) {
        return source.get(name);
    }
}
