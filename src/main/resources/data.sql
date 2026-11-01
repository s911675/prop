-- 데모 데이터 (H2 MERGE: upsert, 멱등)
-- 우선순위: site=myapp > site=default, profile 구체적일수록 나중에 로드되어 최종값이 됨

-- 공통 기본값 (모든 사이트, 모든 프로필)
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('default', 'default', 'app.title',   'APP', '앱 타이틀',  'Default Application');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('default', 'default', 'app.version', 'APP', '앱 버전',   '1.0.0-DB');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('default', 'default', 'app.max-connections', 'DB', '최대 커넥션 수', '10');

-- myapp 사이트 기본값 (site=myapp, profile=default)
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'default', 'app.title',        'APP', '앱 타이틀 (사이트)', 'Prop Demo App');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'default', 'app.description',  'APP', '앱 설명', 'DB Property Source 데모 프로젝트');

-- myapp 개발 환경 (site=myapp, profile=dev) - 활성 프로필이 dev일 때 적용
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'dev', 'app.environment', 'APP', '현재 환경', 'Development');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'dev', 'app.debug',       'APP', '디버그 모드', 'true');

-- myapp 운영 환경 (site=myapp, profile=prod) - 활성 프로필이 prod일 때 적용
MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'prod', 'app.environment', 'APP', '현재 환경', 'Production');

MERGE INTO zt_property_source (site, profile, property_key, se, property_name, property_value)
    KEY (site, profile, property_key)
    VALUES ('myapp', 'prod', 'app.debug',       'APP', '디버그 모드', 'false');
