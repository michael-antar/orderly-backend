package com.orderly.orderly_backend.catalog.dto.req;

import jakarta.validation.constraints.NotBlank;

public record RenameTagRequest(@NotBlank String name) {}
