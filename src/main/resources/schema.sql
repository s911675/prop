-- DB 프라퍼티 소스 테이블
-- DatabasePropertyLoader.ensureTableExists()와 동일 DDL (멱등)
CREATE TABLE IF NOT EXISTS zt_property_source (
    site           VARCHAR(100)  NOT NULL,
    profile        VARCHAR(100)  NOT NULL,
    property_key   VARCHAR(255)  NOT NULL,
    se             VARCHAR(100),
    property_name  VARCHAR(255),
    property_value VARCHAR(2000),
    CONSTRAINT pk_zt_property_source PRIMARY KEY (site, profile, property_key)
);
