package com.orderly.orderly_backend.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String token,
        Instant expiresAt,
        UserSummary user
) {}
