package com.orderly.orderly_backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidation_returns400WithValidationErrorCodeAndFieldsMap() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must be a valid email address"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().fields()).containsEntry("email", "must be a valid email address");
    }

    @Test
    void handleValidation_multipleFieldErrors_allFieldsPresentInMap() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));
        bindingResult.addError(new FieldError("request", "password", "size must be between 8 and 2147483647"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getBody().fields())
                .containsEntry("email", "must not be blank")
                .containsEntry("password", "size must be between 8 and 2147483647");
    }

    @Test
    void handleEmailAlreadyExists_returns409WithEmailAlreadyExistsCode() {
        ResponseEntity<ErrorResponse> response = handler.handleEmailAlreadyExists(new EmailAlreadyExistsException());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(response.getBody().message()).isNotBlank();
        assertThat(response.getBody().fields()).isNull();
    }

    @Test
    void handleInvalidCredentials_returns401WithInvalidCredentialsCode() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().fields()).isNull();
    }

    @Test
    void handleIncorrectPassword_returns401WithIncorrectPasswordCode() {
        ResponseEntity<ErrorResponse> response = handler.handleIncorrectPassword(new IncorrectPasswordException());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("INCORRECT_PASSWORD");
        assertThat(response.getBody().fields()).isNull();
    }

    @Test
    void handleInvalidResetToken_returns400WithInvalidResetTokenCode() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidResetToken(new InvalidResetTokenException());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("INVALID_RESET_TOKEN");
        assertThat(response.getBody().fields()).isNull();
    }

    @Test
    void handleForbidden_returns403WithForbiddenCode() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(new ForbiddenException());

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
        assertThat(response.getBody().fields()).isNull();
    }

    @Test
    void handleNotFound_returns404WithNotFoundCode() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new NotFoundException("Item not found"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Item not found");
        assertThat(response.getBody().fields()).isNull();
    }
}
