# prop 프로젝트

grip-core의 DB 기반 Property Source 로딩/Reload 기능을 독립 Spring Boot 프로젝트로 이식한 데모.
공통코드(그룹코드·상세코드) CRUD 및 Valkey(Redis 호환) 캐싱 기능 포함.

## 목적

DB 테이블(`zt_property_source`)에 저장된 프라퍼티를 앱 기동 시 Spring Environment에 주입하고,
관리 API로 수정 후 앱 재기동 없이 즉시 반영하는 구조를 검증한다.
다중 인스턴스 환경에서 Valkey Pub/Sub으로 PropertySource를 동기화하고,
공통코드를 Valkey에 캐싱하여 DB 부하를 줄인다.

## 기술 스택

- Java 17, Spring Boot 3.3.5
- MyBatis Spring Boot Starter 3.0.3 + MyBatis 3.5.x (원시 — Spring 기동 전 독립 SqlSession)
- Commons DBCP2 (Spring 기동 전 독립 커넥션용) + Commons Pool2 (Lettuce 풀)
- Bean Validation (spring-boot-starter-validation, Jakarta 네임스페이스)
- H2 파일 DB (`./propdb.mv.db`) — 개발용. 운영 전환 시 `db.primary.*` / `db.replica.*` 교체
- MySQL (운영, `mysql-connector-j` optional 의존성 포함)
- Valkey (Redis 호환, Lettuce 클라이언트) — Sentinel HA 설정 포함
- Spring Cache 추상화 (`@Cacheable` / `@CacheEvict`) + RedisCacheManager

## 핵심 클래스

### Property 로딩

| 클래스 | 역할 |
|--------|------|
| `DatabasePropertyLoader` | Spring 기동 초기에 독립 MyBatis + DBCP로 Replica DB 조회 → `MutablePropertySources.addFirst()` |
| `DatabasePropertySourceConfigurer` | `PlaceholderConfigurerSupport` + `EnvironmentAware` + `reload() → int` |
| `PropertyAutoReloadRunner` | `ApplicationRunner` **@Order(1)**: 컨텍스트 완전 초기화 후 자동 reload (data.sql 적용 보장) |
| `PropertiesConfig` | `static @Bean`으로 configurer 등록 + `ignoreUnresolvablePlaceholders=true` |
| `ProfileUtils` | 활성 프로파일·사이트 목록 해석 (LinkedHashSet 중복 제거) |
| `PropertyServiceImpl` | save/delete 후 트랜잭션 커밋 완료 시 `configurer.reload()` + Pub/Sub 발행 |

### DataSource / 라우팅

| 클래스 | 역할 |
|--------|------|
| `RoutingDataSource` | `AbstractRoutingDataSource` — `readOnly` 여부로 Primary/Replica 분기 |
| `DataSourceConfig` | Primary/Replica DataSource 빈 + `LazyConnectionDataSourceProxy` 래핑 |

### 공통코드

| 클래스 | 역할 |
|--------|------|
| `ComGrpCd` | 공통그룹코드 엔티티 (`tb_com_grp_cd`) |
| `ComCd` | 공통코드 엔티티 (`tb_com_cd`) |
| `ComGrpCdMapper` | MyBatis Mapper — `mapper/ComGrpCdMapper.xml` |
| `ComCdMapper` | MyBatis Mapper — `mapper/ComCdMapper.xml` |
| `ComGrpCdServiceImpl` | CRUD + `@Cacheable("grp")` / `@CacheEvict` |
| `ComCdServiceImpl` | CRUD + `@Cacheable("detail")` / `@CacheEvict` |

### Valkey 캐시

| 클래스 | 역할 |
|--------|------|
| `CacheConfig` | `@EnableCaching`; factory 없으면 ConcurrentMap, 있으면 `RuntimeToggleCacheManager` |
| `RuntimeToggleCacheManager` | `getCache()` 호출 시마다 `valkey.enabled` / `cache.code.enabled` 를 `env.getProperty()`로 확인 → 런타임 토글 |
| `FallbackCache` | Redis Cache 데코레이터 — 모든 연산 try-catch; 실패 시 `null` 반환(DB 폴백) 또는 silent skip |
| `CacheWarmupRunner` | `ApplicationRunner` **@Order(2)**: `code:grp:all` 키 존재 확인 후 미존재 시 워밍업 (멀티 인스턴스 중복 방지) |
| `PropertyReloadPublisher` | `prop:reload` 채널 발행 (런타임 플래그 체크 포함) |
| `PropertyReloadListener` | `prop:reload` 채널 구독 → `configurer.reload()` |

### 공통

| 클래스 | 역할 |
|--------|------|
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

## Valkey 캐시 키 체계

```
code:grp:all                   → ComGrpCdService.findAll()       TTL 30분
code:grp:{grpCd}               → ComGrpCdService.findById()      TTL 30분
code:detail:{grpCd}            → ComCdService.findByGrpCd()      TTL 10분
code:detail:{grpCd}:{cd}       → ComCdService.findById()         TTL 10분

pub/sub 채널: prop:reload      → PropertyReloadListener
```

## Valkey 플래그 (런타임 토글 가능)

| 프라퍼티 | 기본 | 설명 |
|----------|------|------|
| `valkey.enabled` | `true` | 마스터 스위치 — false 시 코드 캐시 + pub/sub 모두 비활성화 |
| `cache.code.enabled` | `true` | 공통코드 캐시(`detail`/`grp`) 개별 스위치 |
| `cache.property.pubsub.enabled` | `true` | 프라퍼티 reload 브로드캐스트 (다중 WAS 동기화) |
| `cache.warmup.enabled` | `true` | 기동 시 자동 워밍업 |
| `cache.code.detail.ttl-minutes` | `10` | detail 캐시 TTL |
| `cache.code.grp.ttl-minutes` | `30` | grp 캐시 TTL |

**런타임 비활성화 예시 (재기동 불필요):**
```bash
POST /api/properties
{ "site":"default","profile":"default","propertyKey":"valkey.enabled","propertyValue":"false" }
# → 커밋 완료 즉시 자동 reload → 이후 모든 캐시 연산이 JVM ConcurrentMap으로 전환
```

**Valkey 장애 시 자동 폴백:**
`FallbackCache`가 연결 오류·역직렬화 오류를 흡수 → `null` 반환 → `@Cacheable`이 DB 직접 조회.
서비스는 성능 저하(캐시 미스)만 발생, 중단 없음.

## DataSource 구성 — Primary / Replica 분리

### Spring 관리 DataSource (MyBatis Mapper)

`AbstractRoutingDataSource` + `LazyConnectionDataSourceProxy` 조합으로
트랜잭션 `readOnly` 여부에 따라 자동 분기.

```
@Transactional                → Primary (INSERT / UPDATE / DELETE)
@Transactional(readOnly=true) → Replica  (SELECT)
```

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
db.primary.jdbc-url=jdbc:mysql://DB1_IP:3306/propdb
db.primary.driver-class-name=com.mysql.cj.jdbc.Driver
db.primary.username=appuser
db.primary.password=secret

# Replica DataSource (Read DB)
db.replica.jdbc-url=jdbc:mysql://DB2_IP:3306/propdb
db.replica.driver-class-name=com.mysql.cj.jdbc.Driver
db.replica.username=reader
db.replica.password=secret

# DatabasePropertyLoader 전용 (Spring 기동 전, Replica 사용)
jdbc.url=jdbc:mysql://DB2_IP:3306/propdb
jdbc.driver.class.name=com.mysql.cj.jdbc.Driver
jdbc.username=reader
jdbc.password=secret

# JNDI 사용 시
# jndi.name=java:comp/env/jdbc/propds

grip.site=myapp
# spring.profiles.active=dev

# Valkey — Standalone (dev)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Valkey — Sentinel HA (운영, 아래 주석 해제 후 위 host/port 주석 처리)
# spring.data.redis.sentinel.master=mymaster
# spring.data.redis.sentinel.nodes=WAS1_IP:26379,WAS2_IP:26379,DB1_IP:26379
```

> H2 개발 환경: `jdbc:h2:file:./propdb;AUTO_SERVER=TRUE`를 primary/replica/jdbc 모두 동일하게 설정.
> HikariCP는 `jdbc-url` 키를 사용한다 (`url` 아님).

## 빌드

```bash
# JDK 17 이상 필요 (Spring Boot 3.x)
mvn spring-boot:run
```

기동 로그 확인:
```
[Order 1] ApplicationRunner: reload complete — N keys loaded
[Order 2] Cache warmup done: N groups, N detail entries   # Valkey 활성화 시
[Order 2] Cache already warm, skipping startup warmup     # 두 번째 인스턴스
```

## API

### Property

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/properties` | 전체 목록 (Replica 조회) |
| GET | `/api/properties?site=&profile=&se=` | 필터 조회 |
| POST | `/api/properties` | 저장(upsert) + 자동 reload + Pub/Sub 발행 |
| DELETE | `/api/properties` | 삭제 + 자동 reload + Pub/Sub 발행 |
| POST | `/api/properties/reload` | DB 강제 reload → `{"keysLoaded": N}` |
| GET | `/api/properties/resolve/{key}` | 우선순위 적용 최종값 확인 |
| GET | `/api/properties/categories` | se(카테고리) distinct 목록 |
| GET | `/api/properties/context` | 현재 site·activeProfiles·keysLoaded 확인 |

### 공통그룹코드

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/grp-codes` | 전체/필터 조회 |
| GET | `/api/grp-codes/{grpCd}` | 단건 조회 |
| POST | `/api/grp-codes` | 저장(upsert) |
| DELETE | `/api/grp-codes/{grpCd}` | 삭제 |

### 공통코드

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/codes` | 전체/필터 조회 |
| GET | `/api/codes/{grpCd}` | 그룹별 활성 코드 조회 (캐시) |
| GET | `/api/codes/{grpCd}/{cd}` | 단건 조회 (캐시) |
| POST | `/api/codes` | 저장(upsert) |
| DELETE | `/api/codes/{grpCd}/{cd}` | 삭제 |

### 캐시

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/cache/warmup` | 강제 워밍업 (Valkey 복구·플러시 후) → `{"grpCount":N,"detailCount":N}` |

### 개발

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/h2-console` | H2 콘솔 (개발 전용) |

## 응답 DTO

| DTO | 필드 | 사용 |
|-----|------|------|
| `ApiResponse` | status, message | save, delete |
| `ReloadResponse` | status, message, keysLoaded | reload |
| `ResolveResponse` | key, value, activeProfiles | resolve |
| `ContextResponse` | site, activeProfiles, keysLoaded, sourceName | context |
| `WarmupResponse` | status, message, grpCount, detailCount | cache warmup |

## 테스트

```bash
mvn test -Dspring.profiles.active=test   # 39개 전체 통과
```

| 테스트 클래스 | 수 | 내용 |
|---|---|---|
| `PropertySqlProviderTest` | 10 | SQL 생성 단위 테스트 (Spring 없음) |
| `PropertyServiceImplTest` | 11 | 서비스 통합 테스트 (`@Transactional` 롤백) |
| `PropertyControllerTest` | 13 | MockMvc 통합 테스트 (E2E, 유효성 검증 포함) |
| `DatabasePropertyLoaderTest` | 6 | 로더 동작, 프로파일 중복 방지, 키 카운트 |
| `ProfileUtilsTest` | 9 | 활성 프로파일 해석 단위 테스트 |

테스트 환경: `src/test/resources/application-test.properties`
- In-memory H2 (`jdbc:h2:mem:testdb`)
- `cache.code.enabled=false`, `cache.property.pubsub.enabled=false` (Valkey 비활성화)

## 알려진 이슈 및 주의사항

### @Value 주입 빈은 reload 후 자동 갱신 안 됨
`env.getProperty(key)` 직접 호출 코드만 reload 즉시 반영된다.
`@Value("${key}")` 로 주입된 필드는 빈 초기화 시 한 번만 주입되므로
reload 후에도 기존 값 유지.

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

### 캐시 워밍업 레이스 컨디션
여러 인스턴스가 동시에 기동되면 `code:grp:all` 키 없음 체크 후 모두 워밍업할 수 있다.
동일 데이터를 Valkey에 쓰는 것이므로 무해하며 TTL 내 수렴된다.
