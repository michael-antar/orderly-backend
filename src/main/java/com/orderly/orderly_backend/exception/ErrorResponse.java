package com.orderly.orderly_backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
