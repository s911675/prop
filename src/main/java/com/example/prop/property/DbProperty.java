package com.example.prop.property;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * DB 테이블 zt_property_source 행 매핑 VO.
 * site + profile + property_key 복합 PK.
 */
@Data
public class DbProperty {

    @NotBlank(message = "site는 필수입니다")
    @Size(max = 100)
    private String site;

    @NotBlank(message = "profile은 필수입니다")
    @Size(max = 100)
    private String profile;

    @NotBlank(message = "propertyKey는 필수입니다")
    @Size(max = 255)
    private String propertyKey;

    @Size(max = 100)
    private String se;

    @Size(max = 255)
    private String propertyName;

    @Size(max = 2000)
    private String propertyValue;
}
