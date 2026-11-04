package com.example.prop.code;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ComGrpCd {

    private String grpCd;
    private String grpCdNm;
    private String dc;
    private Integer sortOrd;
    private String useAt;
    private String etc1;
    private LocalDateTime regDt;
    private String rgtr;
    private LocalDateTime mdfcnDt;
    private String mdfr;
}
