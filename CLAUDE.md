# prop 프로젝트

grip-core의 DB 기반 Property Source 로딩/Reload 기능을 독립 Spring Boot 프로젝트로 이식한 데모.

## 목적

DB 테이블(`zt_property_source`)에 저장된 프라퍼티를 앱 기동 시 Spring Environment에 주입하고,
관리 API로 수정 후 앱 재기동 없이 즉시 반영하는 구조를 검증한다.

## 기술 스택

- Java 8, Spring Boot 2.7.18
- MyBatis Spring Boot Starter 2.3.2 + MyBatis 3.5.16 (원시)
- Commons DBCP2 (Spring 기동 전 독립 커넥션용)
- Bean Validation (spring-boot-starter-validation)
- H2 파일 DB (`./propdb.mv.db`) — 운영 전환 시 `db.primary.*` / `db.replica.*` 프라퍼티만 교체

## 핵심 클래스

| 클래스 | 역할 |
|--------|------|
| `DatabasePropertyLoader` | Spring 기동 초기에 독립 MyBatis + DBCP로 Replica DB 조회 → `MutablePropertySources.replace/addFirst()` |
| `DatabasePropertySourceConfigurer` | `PlaceholderConfigurerSupport` + `EnvironmentAware` + `reload() → int` |
| `RoutingDataSource` | `AbstractRoutingDataSource` — readOnly 여부로 Primary/Replica 분기 |
| `DataSourceConfig` | Primary/Replica DataSource 빈 + `LazyConnectionDataSourceProxy` 래핑 |
| `PropertyAutoReloadRunner` | `ApplicationRunner`: 컨텍스트 완전 초기화 후 자동 reload (data.sql 적용 보장) |
| `PropertiesConfig` | `static @Bean`으로 configurer 등록 + `ignoreUnresolvablePlaceholders=true` |
| `ProfileUtils` | 활성 프로파일·사이트 목록 해석 (LinkedHashSet 중복 제거) |
| `PropertyServiceImpl` | save/delete 후 트랜잭션 커밋 완료 시 `configurer.reload()` 자동 호출 |
| `GlobalExceptionHandler` | `@NotBlank` 위반 → 400 + 필드별 오류 메시지 |

## 프라퍼티 우선순위 (높을수록 먼저)

```
[1] DB (zt_property_source) — addFirst()
[2] application.properties
[3] JVM 시스템 프라퍼티
[4] OS 환경변수
```

## DB 로딩 순서 (site × profile 조합)

```
default / default  → 공통 기본값
default / {active} → 공통 프로필별 값
{site}  / default  → 사이트 기본값  (앞 값 덮어씀)
{site}  / {active} → 사이트+프로필 최종값 (최우선)
```

`grip.site=myapp`, `spring.profiles.active=dev` 이면:
`default/default` → `default/dev` → `myapp/default` → `myapp/dev` 순으로 로드.

## DataSource 구성 — Primary / Replica 분리

### Spring 관리 DataSource (MyBatis Mapper)

`AbstractRoutingDataSource` + `LazyConnectionDataSourceProxy` 조합으로
트랜잭션 `readOnly` 여부에 따라 자동 분기.

```
@Transactional                → Primary (INSERT / UPDATE / DELETE)
@Transactional(readOnly=true) → Replica  (SELECT)
```

`LazyConnectionDataSourceProxy`: 커넥션 획득 시점을 트랜잭션 바인딩 이후로 지연해
`readOnly` 플래그 세팅 후 라우팅 결정이 일어남을 보장.

### DatabasePropertyLoader 전용 DataSource

Spring 기동 전(`BeanFactoryPostProcessor` 시점)에 실행되므로 Spring 빈 주입 불가.
`jdbc.*` 프라퍼티로 Commons DBCP2 또는 JNDI DataSource를 독립 생성하며 **Replica를 대상**으로 한다.

```
jndi.name 설정 있음 → JNDI 조회 (jndiSource=true → close() 금지)
실패 또는 없음      → jdbc.url/driver/username/password 로 DBCP2 직접 생성
둘 다 실패          → 경고 로그 후 파일 기반 프라퍼티만 사용 (앱은 정상 기동)
```

## 주요 설정 (application.properties)

```properties
# Primary DataSource (Write DB)
db.primary.jdbc-url=jdbc:postgresql://write-host:5432/propdb
db.primary.driver-class-name=org.postgresql.Driver
db.primary.username=appuser
db.primary.password=secret

# Replica DataSource (Read DB)
db.replica.jdbc-url=jdbc:postgresql://read-host:5432/propdb
db.replica.driver-class-name=org.postgresql.Driver
db.replica.username=reader
db.replica.password=secret

# DatabasePropertyLoader 전용 (Spring 기동 전, Replica 사용)
jdbc.url=jdbc:postgresql://read-host:5432/propdb
jdbc.driver.class.name=org.postgresql.Driver
jdbc.username=reader
jdbc.password=secret

# JNDI 사용 시 (Tomcat: java:comp/env/jdbc/propds, JEUS: jdbc/propds)
# jndi.name=java:comp/env/jdbc/propds

grip.site=myapp
# spring.profiles.active=dev
```

> H2 개발 환경에서는 `jdbc:h2:file:./propdb;AUTO_SERVER=TRUE`를 primary/replica 모두 동일하게 설정.
> HikariCP는 `jdbc-url` 키를 사용한다 (`url` 아님).

## API

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/properties` | 전체 목록 (Replica 조회) |
| GET | `/api/properties?site=&profile=&se=` | 필터 조회 (Replica 조회) |
| POST | `/api/properties` | 저장(upsert) + 자동 reload — `@NotBlank` 검증 |
| DELETE | `/api/properties` | 삭제 + 자동 reload — `@NotBlank` 검증 |
| POST | `/api/properties/reload` | DB 강제 reload → `{"keysLoaded": N}` |
| GET | `/api/properties/resolve/{key}` | 우선순위 적용 최종값 확인 |
| GET | `/api/properties/categories` | se(카테고리) distinct 목록 |
| GET | `/api/properties/context` | 현재 site·activeProfiles·keysLoaded 확인 |
| GET | `/h2-console` | H2 콘솔 (개발 전용) |

## 응답 DTO

| DTO | 사용 엔드포인트 |
|-----|----------------|
| `ApiResponse(status, message)` | save, delete |
| `ReloadResponse(status, message, keysLoaded)` | reload |
| `ResolveResponse(key, value, activeProfiles)` | resolve |
| `ContextResponse(site, activeProfiles, keysLoaded, sourceName)` | context |

## 실행

```bash
mvn spring-boot:run
```

기동 시 0 keys 로그가 보여도 정상 — `PropertyAutoReloadRunner`가 기동 완료 후 자동 reload.
이후 로그에서 `=== Total: N keys loaded ===` 확인.

## 알려진 이슈 및 주의사항

### @Value 주입 빈은 reload 후 자동 갱신 안 됨
`env.getProperty(key)` 직접 호출 코드만 reload 즉시 반영된다.
`@Value("${key}")` 로 주입된 필드는 빈 초기화 시 한 번만 주입되므로
reload 후에도 기존 값 유지. 재적용이 필요하면 ApplicationEvent 처리 필요.

### reload()는 전체 교체
변경된 항목만 부분 갱신이 아닌, DB 전체 재조회 후 PropertySource 통째로 교체.
`synchronized` + `mps.replace()`로 원자적 교체 — 순간적 키 소멸 구간 없음.

### afterCommit reload 복제 지연
`save()`/`delete()` 커밋 후 `afterCommit()`에서 Replica를 재조회하므로
복제 지연(수 ms~수백 ms) 동안 이전 값이 로드될 수 있다.
허용 불가 시 `jdbc.*`를 Primary로 설정.

### data.sql / schema.sql
H2 `MERGE INTO ... KEY (...)` 문법 사용 (멱등 upsert).
`spring.sql.init.mode=always`로 매 기동 시 실행.

### JNDI DataSource close() 금지
JNDI로 획득한 DataSource는 컨테이너 관리이므로 앱에서 `close()` 하면 안 된다.
`jndiSource=true` 플래그로 `closeIfDbcp()` 호출을 차단한다.

## 테스트

```bash
mvn test   # 40개 전체 통과
```

| 테스트 클래스 | 수 | 내용 |
|---|---|---|
| `PropertySqlProviderTest` | 10 | SQL 생성 단위 테스트 (Spring 없음) |
| `PropertyServiceImplTest` | 11 | 서비스 통합 테스트 (`@Transactional` 롤백) |
| `PropertyControllerTest` | 13 | MockMvc 통합 테스트 (E2E, 유효성 검증 포함) |
| `DatabasePropertyLoaderTest` | 6 | 로더 동작, 프로파일 중복 방지, 키 카운트 |

## 원본 (grip-core) 대비 변경사항

| grip-core | 이 프로젝트 |
|-----------|------------|
| `GripPropertySource` | `DbProperty` |
| `DatabasePropertySourcePlaceHolderConfigurer` | `DatabasePropertySourceConfigurer` |
| `SqlProviderUtils` (내부 유틸) | MyBatis `SQL` 빌더 직접 사용 |
| `DbVendorCd` (내부 열거형) | 제거 |
| `Marker` (Log4j2 마커) | SLF4J 직접 사용 |
| `PropertyChangedEventPublisher` | 제거 (필요 시 추가) |
| — | `PropertyAutoReloadRunner` 신규 (기동 후 자동 reload) |
| — | `RoutingDataSource` + `DataSourceConfig` (Primary/Replica 분리) |
| — | `ProfileUtils` (activeProfiles 중복 제거) |
| — | `GlobalExceptionHandler` (@NotBlank → 400) |
| — | 타입 안전 응답 DTO 4종 |
| — | reload() → int 키 카운트 반환 |
