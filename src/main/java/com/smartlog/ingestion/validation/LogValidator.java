package com.smartlog.ingestion.validation;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.ingestion.dto.LogIngestionRequest;

@Component
public class LogValidator {

    private final IngestionValidationProperties properties;

    public LogValidator(IngestionValidationProperties properties) {
        this.properties = properties;
    }

    public void validate(LogIngestionRequest request) {
        try {
            LogLevel.from(request.level());
        } catch (IllegalArgumentException exception) {
            throw new InvalidLogRequestException(exception.getMessage());
        }
        if (request.stackTrace() != null && request.stackTrace().length() > properties.maxStackTraceChars()) {
            throw new PayloadTooLargeException("stackTrace exceeds " + properties.maxStackTraceChars() + " characters");
        }
        int estimatedPayloadBytes = bytes(request.message())
                + bytes(request.stackTrace())
                + bytes(String.valueOf(request.attributes()));
        if (estimatedPayloadBytes > properties.maxPayloadBytes()) {
            throw new PayloadTooLargeException("log payload exceeds " + properties.maxPayloadBytes() + " bytes");
        }
    }

    private int bytes(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }
}
