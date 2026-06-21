package com.orderly.orderly_backend.catalog.dto.req;

import com.orderly.orderly_backend.catalog.ItemStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record UpdateItemRequest(
        @NotBlank String name,
        String description,
        @NotNull ItemStatus status,
        @NotNull Map<String, Object> properties,
        @NotNull @Valid List<ItemTagInputDto> tags
) {}
