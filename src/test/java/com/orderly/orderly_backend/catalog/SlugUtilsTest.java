package com.orderly.orderly_backend.catalog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilsTest {

    @Test
    void simple_label_lowercased_and_spaces_replaced() {
        assertThat(SlugUtils.toSlug("My Field")).isEqualTo("my_field");
    }

    @Test
    void already_valid_slug_unchanged() {
        assertThat(SlugUtils.toSlug("genre")).isEqualTo("genre");
    }

    @Test
    void multiple_spaces_collapse_to_single_underscore() {
        assertThat(SlugUtils.toSlug("price   range")).isEqualTo("price_range");
    }

    @Test
    void special_characters_replaced() {
        assertThat(SlugUtils.toSlug("Price Range (USD)")).isEqualTo("price_range_usd");
    }

    @Test
    void leading_and_trailing_spaces_stripped() {
        assertThat(SlugUtils.toSlug("  director  ")).isEqualTo("director");
    }

    @Test
    void leading_and_trailing_special_chars_stripped() {
        assertThat(SlugUtils.toSlug("--my-field--")).isEqualTo("my_field");
    }

    @Test
    void mixed_separators_collapse() {
        assertThat(SlugUtils.toSlug("release_date (year)")).isEqualTo("release_date_year");
    }

    @Test
    void numbers_preserved() {
        assertThat(SlugUtils.toSlug("Top 10 List")).isEqualTo("top_10_list");
    }

    @Test
    void all_special_chars_returns_empty() {
        assertThat(SlugUtils.toSlug("---!!!---")).isEqualTo("");
    }

    @Test
    void null_returns_empty() {
        assertThat(SlugUtils.toSlug(null)).isEqualTo("");
    }

    @Test
    void empty_string_returns_empty() {
        assertThat(SlugUtils.toSlug("")).isEqualTo("");
    }

    @Test
    void unicode_letters_lowercased_non_ascii_replaced() {
        // Non-ASCII chars (e.g. accented letters) don't match [a-z0-9] and become _
        assertThat(SlugUtils.toSlug("café name")).isEqualTo("caf_name");
    }
}
