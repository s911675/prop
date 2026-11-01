# prop 프로젝트

grip-core의 DB 기반 Property Source 로딩/Reload 기능을 독립 Spring Boot 프로젝트로 이식한 데모.

## 목적

DB 테이블(`zt_property_source`)에 저장된 프라퍼티를 앱 기동 시 Spring Environment에 주입하고,
관리 API로 수정 후 앱 재기동 없이 즉시 반영하는 구조를 검증한다.

## 기술 스택

- Java 8, Spring Boot 2.7.18
- MyBatis Spring Boot Starter 2.3.2 + MyBatis 3.5.16 (원시)
- Commons DBCP2 (Spring 기동 전 독립 커넥션용)
- H2 파일 DB (`./propdb.mv.db`) — 운영 전환 시 `jdbc.*` 프라퍼티만 교체

## 핵심 클래스

| 클래스 | 역할 |
|--------|------|
| `DatabasePropertyLoader` | Spring 기동 초기에 독립 MyBatis + DBCP로 DB 조회 → `MutablePropertySources.addFirst()` |
| `DatabasePropertySourceConfigurer` | `PlaceholderConfigurerSupport` + `EnvironmentAware` + `reload()` |
| `PropertyAutoReloadRunner` | `ApplicationRunner`: 컨텍스트 완전 초기화 후 자동 reload (data.sql 적용 보장) |
| `PropertiesConfig` | `static @Bean`으로 configurer 등록 + `ignoreUnresolvablePlaceholders=true` |
| `PropertyServiceImpl` | save/delete 후 `configurer.reload()` 자동 호출 |

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

## DataSource 획득 전략

```
jndi.name 설정 있음 → JNDI 조회
실패 또는 없음      → jdbc.url/driver/username/password 로 Commons DBCP2 직접 생성
둘 다 실패          → 경고 로그 후 파일 기반 프라퍼티만 사용 (앱은 정상 기동)
```

## 주요 설정 (application.properties)

```properties
# H2 파일 DB (AUTO_SERVER=TRUE: 기동 전 DBCP와 Spring Pool 동시 허용)
spring.datasource.url=jdbc:h2:file:./propdb;AUTO_SERVER=TRUE
jdbc.url=jdbc:h2:file:./propdb;AUTO_SERVER=TRUE
jdbc.driver.class.name=org.h2.Driver
jdbc.username=sa
jdbc.password=

grip.site=myapp          # site 구분자 (DB 조회 조건)
# spring.profiles.active=dev  # 프로필 설정
```

## API

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/properties` | 전체 목록 |
| POST | `/api/properties` | 저장(upsert) + 자동 reload |
| DELETE | `/api/properties` | 삭제 + 자동 reload |
| POST | `/api/properties/reload` | DB 강제 reload |
| GET | `/api/properties/resolve/{key}` | 우선순위 적용 최종값 확인 |
| GET | `/h2-console` | H2 콘솔 (JDBC URL: `jdbc:h2:file:./propdb`) |

## 실행

```bash
mvn spring-boot:run
```

기동 시 0 keys 로그가 보여도 정상 — `PropertyAutoReloadRunner`가 기동 완료 후 자동 reload.
이후 로그에서 `=== Total: N keys loaded ===` 확인.

## 알려진 이슈 및 주의사항

### standalone MyBatis에 mapUnderscoreToCamelCase 필수
`DatabasePropertyLoader`는 Spring과 무관한 독립 MyBatis를 사용하므로
`configuration.setMapUnderscoreToCamelCase(true)`를 명시해야 한다.
없으면 `property_key` → `propertyKey` 매핑 실패 → 모든 키가 null → sourceMap에 1개만 저장.
grip-core 원본에도 동일 잠재 이슈 존재.

### @Value 주입 빈은 reload 후 자동 갱신 안 됨
`env.getProperty(key)` 직접 호출 코드만 reload 즉시 반영된다.
`@Value("${key}")` 로 주입된 필드는 빈 초기화 시 한 번만 주입되므로
reload 후에도 기존 값 유지. 재적용이 필요하면 ApplicationEvent 처리 필요.

### reload()는 전체 교체
변경된 항목만 부분 갱신이 아닌, DB 전체 재조회 후 PropertySource 통째로 교체.

### data.sql / schema.sql
H2 `MERGE INTO ... KEY (...)` 문법 사용 (멱등 upsert).
`spring.sql.init.mode=always`로 매 기동 시 실행.

## 원본 (grip-core) 대비 변경사항

| grip-core | 이 프로젝트 |
|-----------|------------|
| `GripPropertySource` | `DbProperty` |
| `DatabasePropertySourcePlaceHolderConfigurer` | `DatabasePropertySourceConfigurer` |
| `SqlProviderUtils` (내부 유틸) | MyBatis `SQL` 빌더 직접 사용 |
| `DbVendorCd` (내부 열거형) | 제거 |
| `Marker` (Log4j2 마커) | SLF4J 직접 사용 |
| `PropertyChangedEventPublisher` | 제거 (필요 시 추가) |
| `PropertyAutoReloadRunner` | 신규 추가 (기동 후 자동 reload) |
