package com.orderly.orderly_backend.catalog;

import java.util.List;

/**
 * Describes a single custom property field on items within a category.
 * Stored as an element of the {@code field_definitions} JSONB array on {@link CategoryDefinition}.
 *
 * <p>{@code key} is derived server-side from {@code label} via slug conversion on creation
 * (e.g. "My Field" → "my_field") and is immutable afterwards. It is the stable key used
 * in {@code items.properties} JSONB and must never be changed after items exist for the category.
 *
 * <p>{@code type} is stored as a plain string (not a Java enum) to avoid JSONB serialization
 * complexity. Valid values are: {@code string}, {@code number}, {@code boolean},
 * {@code select}, {@code date}, {@code location}. The server does not validate item property
 * values against this type — enforcement is a client-side concern.
 *
 * <p>{@code options} is only meaningful when {@code type} is {@code "select"}.
 */
public record FieldDefinition(
        String key,
        String label,
        String type,
        List<String> options,
        Boolean required
) {}
