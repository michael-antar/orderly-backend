package com.orderly.orderly_backend.catalog.dto.resp;

/** Lightweight tag projection used inside item responses. See {@link TagDetailDto} for the full form. */
public record TagDto(Long id, String name) {}
