package com.orderly.orderly_backend.auth;

import com.orderly.orderly_backend.auth.dto.AuthResponse;
import com.orderly.orderly_backend.auth.dto.ChangePasswordRequest;
import com.orderly.orderly_backend.auth.dto.LoginRequest;
import com.orderly.orderly_backend.auth.dto.MessageResponse;
import com.orderly.orderly_backend.auth.dto.PasswordResetConfirmRequest;
import com.orderly.orderly_backend.auth.dto.PasswordResetRequest;
import com.orderly.orderly_backend.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UsedResetTokenRepository usedResetTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public AuthResponse register(RegisterRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public AuthResponse login(LoginRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public MessageResponse requestPasswordReset(PasswordResetRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public MessageResponse changePassword(ChangePasswordRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
