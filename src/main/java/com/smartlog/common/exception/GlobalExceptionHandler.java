package com.smartlog.common.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.smartlog.ingestion.pipeline.LogQueueFullException;
import com.smartlog.ingestion.validation.InvalidLogRequestException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return badRequest(details);
    }

    @ExceptionHandler(InvalidLogRequestException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidLogRequest(InvalidLogRequestException exception) {
        return badRequest(List.of(exception.getMessage()));
    }

    @ExceptionHandler(LogQueueFullException.class)
    ResponseEntity<ApiErrorResponse> handleQueueFull(LogQueueFullException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse("INGESTION_QUEUE_FULL", List.of(exception.getMessage()), Instant.now()));
    }

    private ResponseEntity<ApiErrorResponse> badRequest(List<String> details) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_REQUEST", details, Instant.now()));
    }
}
