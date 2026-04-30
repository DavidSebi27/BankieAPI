package com.bankie.bankie_api.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponse(int status, String message, Instant timestamp) {
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, Instant.now());
    }
}