package com.example.prop.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ContextResponse {
    private final String site;
    private final List<String> activeProfiles;
    private final int keysLoaded;
    private final String sourceName;
}
