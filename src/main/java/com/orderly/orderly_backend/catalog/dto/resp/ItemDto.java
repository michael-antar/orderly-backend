package com.orderly.orderly_backend.catalog.dto.resp;

import com.orderly.orderly_backend.catalog.ItemStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ItemDto(
        UUID id,
        UUID categoryDefId,
        String name,
        String description,
        ItemStatus status,
        BigDecimal rating,
        BigDecimal rd,
        int comparisonCount,
        Instant createdAt,
        Instant lastComparedAt,
        Map<String, Object> properties,
        List<TagDto> tags
) {}
