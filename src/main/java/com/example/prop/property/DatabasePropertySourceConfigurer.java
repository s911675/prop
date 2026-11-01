package com.example.prop.property;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.StringValueResolver;

import java.util.Properties;

/**
 * DB 프라퍼티 소스를 Spring Environment에 등록하고 ${...} 플레이스홀더를 처리한다.
 *
 * 생명주기:
 *   1. setEnvironment() → DatabasePropertyLoader 생성 → DB에서 프라퍼티 로드 → MutablePropertySources 최상위 등록
 *   2. postProcessBeanFactory() → 전체 PropertySources 기반으로 ${...} 치환
 *   3. reload() → DB 재조회 후 PropertySource 교체 (앱 재기동 불필요)
 *
 * PropertiesConfig에서 static @Bean으로 등록해야 BeanFactoryPostProcessor가 조기 초기화된다.
 */
public class DatabasePropertySourceConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

    private org.springframework.core.env.Environment env;
    private DatabasePropertyLoader loader;
    private MutablePropertySources propertySources;

    @Override
    public void setEnvironment(org.springframework.core.env.Environment env) {
        this.env = env;
        this.loader = new DatabasePropertyLoader(env);
        loader.loadDataBasePropertySource();  // 초기 로드: int 반환값 무시 (로그로 확인)
    }

    /**
     * DB 프라퍼티 재로딩 (앱 재기동 없이 반영).
     * 관리 화면에서 프라퍼티 저장/삭제 후 또는 reload API 호출 시 실행.
     *
     * @return 로드된 키 수 (실패 시 -1)
     */
    public int reload() {
        return loader.reload();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (propertySources == null) {
            propertySources = ((ConfigurableEnvironment) env).getPropertySources();
        }
        ConfigurablePropertyResolver resolver = new PropertySourcesPropertyResolver(propertySources);
        resolver.setPlaceholderPrefix(this.placeholderPrefix);
        resolver.setPlaceholderSuffix(this.placeholderSuffix);
        resolver.setValueSeparator(this.valueSeparator);

        StringValueResolver valueResolver = strVal -> {
            String resolved = this.ignoreUnresolvablePlaceholders
                    ? resolver.resolvePlaceholders(strVal)
                    : resolver.resolveRequiredPlaceholders(strVal);
            if (this.trimValues) resolved = resolved.trim();
            return resolved.equals(this.nullValue) ? null : resolved;
        };

        doProcessProperties(beanFactory, valueResolver);
    }

    @Override
    @Deprecated
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
        throw new UnsupportedOperationException(
                "Use processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver)");
    }
}
