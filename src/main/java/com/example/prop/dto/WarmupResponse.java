package com.example.prop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WarmupResponse {
    private String status;
    private String message;
    private int grpCount;
    private int detailCount;
}
