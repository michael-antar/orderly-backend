package com.orderly.orderly_backend.exception;

import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fields
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }
}
