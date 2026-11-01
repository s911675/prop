package com.example.prop.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiResponse {
    private final String status;
    private final String message;

    public static ApiResponse ok(String message) {
        return new ApiResponse("ok", message);
    }
}
