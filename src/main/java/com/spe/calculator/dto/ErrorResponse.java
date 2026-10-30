package com.spe.calculator.dto;

import java.time.Instant;

/**
 * Structured error body returned with HTTP 400.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
