package com.orderly.orderly_backend.catalog.dto.req;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateFieldDefinitionDto(
        @NotBlank String label,
        @NotBlank String type,
        List<String> options,
        Boolean required
) {}
