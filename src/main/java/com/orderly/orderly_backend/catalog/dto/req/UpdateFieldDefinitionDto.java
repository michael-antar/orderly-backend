package com.orderly.orderly_backend.catalog.dto.req;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateFieldDefinitionDto(
        /** Null for new fields — the server derives the key from label via slug conversion. */
        String key,
        @NotBlank String label,
        @NotBlank String type,
        List<String> options,
        Boolean required
) {}
