package com.orderly.orderly_backend.catalog.dto.resp;

import java.util.List;

public record FieldDefinitionDto(
        String key,
        String label,
        String type,
        List<String> options,
        Boolean required
) {}
