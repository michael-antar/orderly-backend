package com.orderly.orderly_backend.catalog.dto.resp;

/** Full tag projection returned by tag management endpoints. Includes usageCount. */
public record TagDetailDto(Long id, String name, long usageCount) {}
