package com.orderly.orderly_backend.catalog.dto.req;

import jakarta.validation.constraints.NotNull;

public record MergeTagsRequest(
        @NotNull Long sourceId,
        @NotNull Long targetId
) {}
