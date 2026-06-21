package com.orderly.orderly_backend.catalog.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCategoryRequest(
        @NotBlank String name,
        String icon,
        @NotNull @Valid List<CreateFieldDefinitionDto> fieldDefinitions
) {}
