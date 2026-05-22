package com.orderly.orderly_backend.auth;

import com.orderly.orderly_backend.auth.dto.AuthResponse;
import com.orderly.orderly_backend.auth.dto.LoginRequest;
import com.orderly.orderly_backend.auth.dto.MessageResponse;
import com.orderly.orderly_backend.auth.dto.PasswordResetConfirmRequest;
import com.orderly.orderly_backend.auth.dto.PasswordResetRequest;
import com.orderly.orderly_backend.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // JWT is stateless — no server-side invalidation needed.
        // Spring Security has already validated the token before reaching here.
        // A token blocklist will be added here in the future without any client changes.
    }

    @PostMapping("/password-reset")
    public MessageResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    public MessageResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return authService.confirmPasswordReset(request);
    }
}
