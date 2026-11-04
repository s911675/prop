package com.example.prop.property;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring 기동 초기(EnvironmentAware.setEnvironment() 시점)에 DB에서 프라퍼티를 읽어
 * MutablePropertySources 최상위에 등록한다.
 *
 * 개선 사항:
 * - synchronized: 동시 reload 시 remove/replace 레이스 컨디션 방지
 * - mps.replace(): 원자적 교체로 순간적 PropertySource 소멸 구간 제거
 * - SqlSessionFactory 캐싱: reload마다 커넥션 풀+팩토리 재생성 비용 제거
 * - AtomicBoolean tableEnsured: CREATE TABLE IF NOT EXISTS 최초 1회만 실행
 * - ProfileUtils: activeProfiles/activeSites 공통 로직 위임
 */
@Slf4j
public class DatabasePropertyLoader {

    private static final String SOURCE_NAME = "DatabasePropertySource";

    private final ConfigurableEnvironment env;

    private SqlSessionFactory cachedFactory;
    private boolean jndiSource = false;
    private final AtomicBoolean tableEnsured = new AtomicBoolean(false);

    public DatabasePropertyLoader(org.springframework.core.env.Environment env) {
        this.env = (ConfigurableEnvironment) env;
    }

    public synchronized int loadDataBasePropertySource() {
        try {
            Map<String, Object> sourceMap = buildSourceMap();
            MutablePropertySources mps = env.getPropertySources();
            DatabasePropertySource newSource = new DatabasePropertySource(SOURCE_NAME, sourceMap);
            if (mps.contains(SOURCE_NAME)) {
                mps.replace(SOURCE_NAME, newSource);  // 원자적 교체
            } else {
                mps.addFirst(newSource);
            }
            log.info("DB property source loaded: {} keys", sourceMap.size());
            return sourceMap.size();
        } catch (RuntimeException e) {
            log.warn("DB property loading failed, using file-based properties only", e);
            return -1;
        }
    }

    public int reload() {
        return loadDataBasePropertySource();
    }

    // -------------------------------------------------------------------------

    private Map<String, Object> buildSourceMap() {
        SqlSessionFactory factory = getOrCreateFactory();
        if (factory == null) throw new RuntimeException("No DataSource available");

        Map<String, Object> result = new HashMap<>();
        log.info("=== DB Property Loading ===");
        try (SqlSession session = factory.openSession()) {
            for (String site : ProfileUtils.resolveSites(env)) {
                for (String profile : ProfileUtils.resolveProfiles(env)) {
                    DbProperty cond = new DbProperty();
                    cond.setSite(site);
                    cond.setProfile(profile);
                    List<DbProperty> list = session.getMapper(PropertyMapper.class).findAll(cond);
                    log.info("  [site={}, profile={}] {} keys", site, profile, list.size());
                    for (DbProperty p : list) {
                        result.put(p.getPropertyKey(), p.getPropertyValue());
                    }
                }
            }
        }
        log.info("=== Total: {} keys loaded ===", result.size());
        return result;
    }

    /**
     * SqlSessionFactory를 최초 1회 생성하고 이후 재사용한다.
     * 이미 synchronized 블록 안에서 호출되므로 추가 동기화 불필요.
     */
    private SqlSessionFactory getOrCreateFactory() {
        if (cachedFactory != null) return cachedFactory;

        DataSource dataSource = resolveDataSource();
        if (dataSource == null) return null;

        ensureTableExistsOnce(dataSource);
        try {
            cachedFactory = createFactory(dataSource);
        } catch (Exception e) {
            closeIfDbcp(dataSource);
            throw new RuntimeException("Failed to create SqlSessionFactory", e);
        }
        return cachedFactory;
    }

    private SqlSessionFactory createFactory(DataSource dataSource) throws Exception {
        TransactionFactory txFactory = new JdbcTransactionFactory();
        Environment ibatisEnv = new Environment("prop-loader", txFactory, dataSource);
        Configuration config = new Configuration(ibatisEnv);
        config.setMapUnderscoreToCamelCase(true);
        try (InputStream is = getClass().getResourceAsStream("/mapper/PropertyMapper.xml")) {
            XMLMapperBuilder parser = new XMLMapperBuilder(
                    is, config, "mapper/PropertyMapper.xml", config.getSqlFragments());
            parser.parse();
        }
        return new SqlSessionFactoryBuilder().build(config);
    }

    private void ensureTableExistsOnce(DataSource dataSource) {
        if (!tableEnsured.compareAndSet(false, true)) return;
        String ddl = "CREATE TABLE IF NOT EXISTS zt_property_source (" +
                     "site          VARCHAR(100) NOT NULL, " +
                     "profile       VARCHAR(100) NOT NULL, " +
                     "property_key  VARCHAR(255) NOT NULL, " +
                     "se            VARCHAR(100), " +
                     "property_name VARCHAR(255), " +
                     "property_value VARCHAR(2000), " +
                     "CONSTRAINT pk_zt_property_source PRIMARY KEY (site, profile, property_key))";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            log.warn("ensureTableExists: {}", e.getMessage());
            tableEnsured.set(false);  // 다음 호출 시 재시도
        }
    }

    private DataSource resolveDataSource() {
        DataSource ds = jndiDataSource();
        if (ds != null) {
            jndiSource = true;
            return ds;
        }
        return dbcpDataSource();
    }

    private DataSource jndiDataSource() {
        String jndiName = env.getProperty("jndi.name");
        if (jndiName == null) return null;
        try {
            log.info("Trying JNDI: {}", jndiName);
            return new JndiDataSourceLookup().getDataSource(jndiName);
        } catch (DataSourceLookupFailureException e) {
            log.info("JNDI not available, falling back to DBCP. jndi={}", jndiName);
            return null;
        }
    }

    private DataSource dbcpDataSource() {
        String url      = env.getProperty("jdbc.url");
        String driver   = env.getProperty("jdbc.driver.class.name");
        String username = env.getProperty("jdbc.username");
        String password = env.getProperty("jdbc.password", "");
        if (!StringUtils.hasLength(url)) {
            log.warn("jdbc.url not configured, DB property loading disabled");
            return null;
        }
        log.info("Creating DBCP datasource: url={}", url);
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName(driver);
        bds.setUrl(url);
        bds.setUsername(username);
        bds.setPassword(password);
        bds.setInitialSize(1);
        bds.setMaxTotal(2);
        bds.setValidationQuery("SELECT 1");
        bds.setTestOnBorrow(true);
        return bds;
    }

    private void closeIfDbcp(DataSource dataSource) {
        if (!jndiSource && dataSource instanceof Closeable) {
            try { ((Closeable) dataSource).close(); } catch (IOException e) { /* ignore */ }
        }
    }
}
