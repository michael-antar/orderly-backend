package com.orderly.orderly_backend.exception;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("The password reset token is invalid or has expired.");
    }
}
