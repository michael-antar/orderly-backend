package com.orderly.orderly_backend.auth;

import com.orderly.orderly_backend.auth.api.UserRegisteredEvent;
import com.orderly.orderly_backend.auth.dto.AuthResponse;
import com.orderly.orderly_backend.auth.dto.ChangePasswordRequest;
import com.orderly.orderly_backend.auth.dto.LoginRequest;
import com.orderly.orderly_backend.auth.dto.MessageResponse;
import com.orderly.orderly_backend.auth.dto.PasswordResetConfirmRequest;
import com.orderly.orderly_backend.auth.dto.PasswordResetRequest;
import com.orderly.orderly_backend.auth.dto.RegisterRequest;
import com.orderly.orderly_backend.auth.dto.UserSummary;
import com.orderly.orderly_backend.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Duplicate email check
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        // Build new user with hashed password
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);

        // Synchronous call to populate user categories
        // Fail and roll back transaction if this fails since we don't want users without categories
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));

        // Return JWT to user
        AuthTokenResult result = jwtService.issueAuthToken(user);
        return new AuthResponse(
                result.token(),
                result.expiresAt(),
                new UserSummary(user.getId(), user.getEmail())
        );
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
