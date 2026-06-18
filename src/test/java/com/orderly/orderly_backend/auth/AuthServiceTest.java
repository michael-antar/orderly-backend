package com.orderly.orderly_backend.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.orderly.orderly_backend.auth.api.UserRegisteredEvent;
import com.orderly.orderly_backend.auth.dto.*;
import com.orderly.orderly_backend.exception.EmailAlreadyExistsException;
import com.orderly.orderly_backend.exception.IncorrectPasswordException;
import com.orderly.orderly_backend.exception.InvalidCredentialsException;
import com.orderly.orderly_backend.exception.InvalidResetTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UsedResetTokenRepository usedResetTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    void register_emailAlreadyExists_throwsEmailAlreadyExistsException() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("user@example.com", "password123")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_success_savesUserPublishesEventAndReturnsAuthResponse() {
        Instant expiry = Instant.now().plusSeconds(604800);
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.issueAuthToken(any(User.class))).thenReturn(new AuthTokenResult("token-string", expiry));

        AuthResponse response = authService.register(new RegisterRequest("user@example.com", "password123"));

        assertThat(response.token()).isEqualTo("token-string");
        assertThat(response.expiresAt()).isEqualTo(expiry);
        assertThat(response.user().email()).isEqualTo("user@example.com");
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void register_success_passwordIsEncodedBeforeSave() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(jwtService.issueAuthToken(any())).thenReturn(new AuthTokenResult("t", Instant.now()));

        authService.register(new RegisterRequest("user@example.com", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("password123");
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    void login_validCredentials_returnsAuthResponse() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com").passwordHash("hashed").build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.issueAuthToken(user)).thenReturn(new AuthTokenResult("token", Instant.now()));

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.token()).isEqualTo("token");
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com").passwordHash("hashed").build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentialsException() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_passwordEncoderStillCalledForTimingConsistency() {
        // When the user does not exist, the service must still call passwordEncoder.matches
        // with a dummy hash so response time does not reveal whether the email is registered.
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder).matches(eq("password123"), anyString());
    }

    // -------------------------------------------------------------------------
    // requestPasswordReset
    // -------------------------------------------------------------------------

    @Test
    void requestPasswordReset_knownEmail_callsEmailServiceWithCorrectArgs() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com").passwordHash("h").build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.issueResetToken(user)).thenReturn("reset-token");

        MessageResponse response = authService.requestPasswordReset(new PasswordResetRequest("user@example.com"));

        verify(emailService).sendPasswordReset("user@example.com", "reset-token");
        assertThat(response.message()).isNotBlank();
    }

    @Test
    void requestPasswordReset_unknownEmail_doesNotCallEmailService() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset(new PasswordResetRequest("nobody@example.com"));

        verify(emailService, never()).sendPasswordReset(any(), any());
    }

    @Test
    void requestPasswordReset_responseMessageIsIdenticalForKnownAndUnknownEmail() {
        User user = User.builder()
                .id(UUID.randomUUID()).email("user@example.com").passwordHash("h").build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.issueResetToken(user)).thenReturn("token");
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        String knownMessage   = authService.requestPasswordReset(new PasswordResetRequest("user@example.com")).message();
        String unknownMessage = authService.requestPasswordReset(new PasswordResetRequest("nobody@example.com")).message();

        assertThat(knownMessage).isEqualTo(unknownMessage);
    }

    // -------------------------------------------------------------------------
    // confirmPasswordReset
    // -------------------------------------------------------------------------

    @Test
    void confirmPasswordReset_validUnusedToken_updatesPasswordAndMarksJtiUsed() {
        UUID userId = UUID.randomUUID();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .jwtID("test-jti")
                .build();
        User user = User.builder().id(userId).email("user@example.com").passwordHash("old-hash").build();
        when(jwtService.parseResetToken("valid-token")).thenReturn(claims);
        when(usedResetTokenRepository.existsByJti("test-jti")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        MessageResponse response = authService.confirmPasswordReset(
                new PasswordResetConfirmRequest("valid-token", "newPassword1"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("new-hash");

        ArgumentCaptor<UsedResetToken> tokenCaptor = ArgumentCaptor.forClass(UsedResetToken.class);
        verify(usedResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getJti()).isEqualTo("test-jti");

        assertThat(response.message()).isNotBlank();
    }

    @Test
    void confirmPasswordReset_replayedJti_throwsBeforePasswordUpdate() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .jwtID("used-jti")
                .build();
        when(jwtService.parseResetToken("token")).thenReturn(claims);
        when(usedResetTokenRepository.existsByJti("used-jti")).thenReturn(true);

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new PasswordResetConfirmRequest("token", "newPassword1")))
                .isInstanceOf(InvalidResetTokenException.class);
        verify(userRepository, never()).save(any());
        verify(usedResetTokenRepository, never()).save(any());
    }

    @Test
    void confirmPasswordReset_invalidToken_propagatesInvalidResetTokenException() {
        when(jwtService.parseResetToken("bad-token")).thenThrow(new InvalidResetTokenException());

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new PasswordResetConfirmRequest("bad-token", "newPassword1")))
                .isInstanceOf(InvalidResetTokenException.class);
        verify(usedResetTokenRepository, never()).existsByJti(any());
    }

    @Test
    void confirmPasswordReset_userNotFoundBySubClaim_throwsInvalidResetTokenException() {
        UUID userId = UUID.randomUUID();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .jwtID("jti-xyz")
                .build();
        when(jwtService.parseResetToken("token")).thenReturn(claims);
        when(usedResetTokenRepository.existsByJti("jti-xyz")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new PasswordResetConfirmRequest("token", "newPassword1")))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    void changePassword_correctCurrentPassword_updatesHashAndReturnsSuccess() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").passwordHash("old-hash").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        MessageResponse response = authService.changePassword(
                new ChangePasswordRequest("current", "newPassword1"), userId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-hash");
        assertThat(response.message()).isNotBlank();
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsIncorrectPasswordException() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").passwordHash("old-hash").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(
                new ChangePasswordRequest("wrong", "newPassword1"), userId))
                .isInstanceOf(IncorrectPasswordException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throwsIllegalStateException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(
                new ChangePasswordRequest("current", "newPassword1"), userId))
                .isInstanceOf(IllegalStateException.class);
    }
}
