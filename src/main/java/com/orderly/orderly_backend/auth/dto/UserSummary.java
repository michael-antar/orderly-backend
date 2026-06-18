package com.orderly.orderly_backend.auth.dto;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String email
) {}
