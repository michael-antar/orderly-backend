package com.orderly.orderly_backend.auth;

import java.time.Instant;

public record AuthTokenResult(String token, Instant expiresAt) {}
