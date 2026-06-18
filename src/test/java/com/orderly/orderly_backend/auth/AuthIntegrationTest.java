package com.orderly.orderly_backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderly.orderly_backend.auth.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource") // Suppresses IDE false positive for unclosed AutoCloseable
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .waitingFor(
                    new WaitAllStrategy()
                            .withStrategy(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                            .withStrategy(Wait.forListeningPort())
                            .withStartupTimeout(Duration.ofSeconds(60))
            );

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean EmailService emailService;

    private static final String BASE = "/api/v1/auth";
    private static final String EMAIL    = "integration@example.com";
    private static final String PASSWORD = "Password1!";

    @BeforeEach
    void resetMocks() {
        reset(emailService);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, password))))
                .andExpect(status().isCreated())
                .andReturn();
        return extractToken(result);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return extractToken(result);
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @Test
    void register_success_returns201WithTokenAndUserSummary() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(EMAIL, PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.id").isNotEmpty());
    }

    @Test
    void register_success_userPersistedInDatabase() throws Exception {
        register(EMAIL, PASSWORD);

        assertThat(userRepository.existsByEmailIgnoreCase(EMAIL)).isTrue();
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(EMAIL, PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void register_duplicateEmailCaseInsensitive_returns409() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(EMAIL.toUpperCase(), PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void register_invalidEmail_returns400ValidationError() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("not-an-email", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").isNotEmpty());
    }

    @Test
    void register_shortPassword_returns400ValidationError() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(EMAIL, "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.password").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(EMAIL, "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("nobody@example.com", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_wrongPasswordAndUnknownEmail_responseIdentical() throws Exception {
        register(EMAIL, PASSWORD);

        MvcResult knownBadPassword = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(EMAIL, "wrongpassword"))))
                .andReturn();

        MvcResult unknownEmail = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("nobody@example.com", PASSWORD))))
                .andReturn();

        assertThat(knownBadPassword.getResponse().getStatus())
                .isEqualTo(unknownEmail.getResponse().getStatus());
        String body1 = objectMapper.readTree(knownBadPassword.getResponse().getContentAsString()).get("error").asText();
        String body2 = objectMapper.readTree(unknownEmail.getResponse().getContentAsString()).get("error").asText();
        assertThat(body1).isEqualTo(body2);
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Test
    void logout_withValidToken_returns204() throws Exception {
        String token = register(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE + "/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(BASE + "/logout"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Full register → login → change password → login with new password
    // -------------------------------------------------------------------------

    @Test
    void fullFlow_registerLoginChangePasswordLoginWithNew() throws Exception {
        // Register
        register(EMAIL, PASSWORD);

        // Login with original password
        String token = login(EMAIL, PASSWORD);
        assertThat(token).isNotBlank();

        // Change password
        String newPassword = "NewPassword2!";
        mockMvc.perform(post(BASE + "/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangePasswordRequest(PASSWORD, newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Old password no longer works
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isUnauthorized());

        // New password works
        login(EMAIL, newPassword);
    }

    // -------------------------------------------------------------------------
    // Change password
    // -------------------------------------------------------------------------

    @Test
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        String token = register(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE + "/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangePasswordRequest("wrongpassword", "NewPassword2!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INCORRECT_PASSWORD"));
    }

    @Test
    void changePassword_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(BASE + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangePasswordRequest(PASSWORD, "NewPassword2!"))))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Password reset — full flow
    // -------------------------------------------------------------------------

    @Test
    void passwordReset_fullFlow_confirmAndLoginWithNewPassword() throws Exception {
        register(EMAIL, PASSWORD);

        // Request reset — always 200 regardless of email existence
        mockMvc.perform(post(BASE + "/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetRequest(EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Capture the reset token sent to the email service
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordReset(eq(EMAIL), tokenCaptor.capture());
        String resetToken = tokenCaptor.getValue();

        // Confirm reset with captured token
        String newPassword = "ResetPassword3!";
        mockMvc.perform(post(BASE + "/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetConfirmRequest(resetToken, newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Login with new password succeeds
        login(EMAIL, newPassword);

        // Login with old password fails
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordReset_unknownEmail_returns200AndEmailServiceNotCalled() throws Exception {
        mockMvc.perform(post(BASE + "/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetRequest("nobody@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(emailService, never()).sendPasswordReset(any(), any());
    }

    @Test
    void passwordReset_responseIdenticalForKnownAndUnknownEmail() throws Exception {
        register(EMAIL, PASSWORD);

        MvcResult knownResult = mockMvc.perform(post(BASE + "/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetRequest(EMAIL))))
                .andReturn();

        MvcResult unknownResult = mockMvc.perform(post(BASE + "/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetRequest("nobody@example.com"))))
                .andReturn();

        String msg1 = objectMapper.readTree(knownResult.getResponse().getContentAsString()).get("message").asText();
        String msg2 = objectMapper.readTree(unknownResult.getResponse().getContentAsString()).get("message").asText();
        assertThat(msg1).isEqualTo(msg2);
    }

    // -------------------------------------------------------------------------
    // Password reset — error cases
    // -------------------------------------------------------------------------

    @Test
    void passwordResetConfirm_invalidToken_returns400() throws Exception {
        mockMvc.perform(post(BASE + "/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetConfirmRequest("not.a.valid.jwt", "NewPassword3!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void passwordResetConfirm_authTokenUsedAsResetToken_returns400() throws Exception {
        // Auth tokens have no purpose claim — must be rejected at reset endpoint
        String authToken = register(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE + "/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetConfirmRequest(authToken, "NewPassword3!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void passwordResetConfirm_replayAttempt_returns400() throws Exception {
        register(EMAIL, PASSWORD);

        // Request reset and capture token
        mockMvc.perform(post(BASE + "/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetRequest(EMAIL))))
                .andReturn();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordReset(eq(EMAIL), tokenCaptor.capture());
        String resetToken = tokenCaptor.getValue();

        // First use — succeeds
        mockMvc.perform(post(BASE + "/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetConfirmRequest(resetToken, "NewPassword3!"))))
                .andExpect(status().isOk());

        // Replay — fails
        mockMvc.perform(post(BASE + "/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PasswordResetConfirmRequest(resetToken, "AnotherPassword4!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_RESET_TOKEN"));
    }

    // -------------------------------------------------------------------------
    // Protected endpoints return 401 without a token
    // -------------------------------------------------------------------------

    @Test
    void protectedEndpoints_withoutToken_allReturn401() throws Exception {
        mockMvc.perform(post(BASE + "/logout")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ChangePasswordRequest(PASSWORD, "NewPassword2!"))))
                .andExpect(status().isUnauthorized());
    }
}
