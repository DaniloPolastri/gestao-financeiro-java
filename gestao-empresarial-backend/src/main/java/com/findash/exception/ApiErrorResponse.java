package com.findash.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    int status,
    String message,
    List<FieldError> errors,
    Instant timestamp
) {
    public record FieldError(String field, String message) {}

    public static ApiErrorResponse of(int status, String message) {
        return new ApiErrorResponse(status, message, List.of(), Instant.now());
    }

    public static ApiErrorResponse of(int status, String message, List<FieldError> errors) {
        return new ApiErrorResponse(status, message, errors, Instant.now());
    }
}
