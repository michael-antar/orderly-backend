package com.orderly.orderly_backend.catalog;

/**
 * Creates URL-safe key from human label. Used to compute {@link FieldDefinition#key} on category creation.
 * <p>
 * Converts to lowercase, replaces non-alphanumerics with single underscores, and strips leading and trailing underscores
 * <p>
 * NOTE: non-ASCII like accents on letters get replaced with underscores.
 */
class SlugUtils {

    private SlugUtils() {}

    static String toSlug(String label) {
        if (label == null) return "";
        return label
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
