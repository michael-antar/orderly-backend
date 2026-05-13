package com.orderly.orderly_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank String token,
        @Size(min = 8) @NotBlank String newPassword
) {}
