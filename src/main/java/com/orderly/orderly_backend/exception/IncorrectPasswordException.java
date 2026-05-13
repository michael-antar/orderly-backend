package com.orderly.orderly_backend.exception;

public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException() {
        super("The current password is incorrect.");
    }
}
