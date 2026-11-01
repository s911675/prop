package com.example.prop.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ReloadResponse {
    private final String status;
    private final String message;
    private final int keysLoaded;

    public static ReloadResponse ok(int keysLoaded) {
        return new ReloadResponse("ok", "reload 완료", keysLoaded);
    }
}
