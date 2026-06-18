package com.orderly.orderly_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @Email @NotBlank String email
) {}
