package com.orderly.orderly_backend.catalog.dto.req;

/**
 * Identifies a tag to associate with an item.
 * Supply {@code id} to reference an existing tag, or {@code name} to create/find one by name.
 * If both are supplied, {@code id} takes precedence.
 * The server deduplicates name-based lookups case-insensitively within the category.
 */
public record ItemTagInputDto(
        Long id,
        String name
) {}
