package com.memoryassistant.config;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Validation errors from @Valid / @NotBlank / @Size on request DTOs */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), null);
    }

    /** Groq/external API failures — surface as 502 so the frontend knows it's upstream */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String requestId = UUID.randomUUID().toString();
        log.error("[{}] Unhandled runtime exception: {}", requestId, ex.getMessage(), ex);
        // Distinguish Groq errors (have a descriptive message) from truly unexpected ones
        String msg = ex.getMessage() != null && ex.getMessage().startsWith("Groq API error")
                ? "AI service error — please try again shortly."
                : "An unexpected error occurred.";
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", msg, requestId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        String requestId = UUID.randomUUID().toString();
        log.error("[{}] Unhandled exception: {}", requestId, ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", requestId);
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String code, String message, String requestId) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "requestId", requestId != null ? requestId : ""
                )
        ));
    }
}
