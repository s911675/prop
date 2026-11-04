package com.example.prop.code;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ComCd {

    private String grpCd;
    private String cd;
    private String cdNm;
    private String dc;
    private Integer sortOrd;
    private String useAt;
    private String etc1;
    private LocalDateTime regDt;
    private String rgtr;
    private LocalDateTime mdfcnDt;
    private String mdfr;
}
