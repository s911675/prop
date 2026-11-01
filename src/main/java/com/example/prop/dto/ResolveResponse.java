package com.example.prop.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ResolveResponse {
    private final String key;
    private final String value;
    private final List<String> activeProfiles;
}
