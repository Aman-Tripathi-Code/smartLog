package com.smartlog.ingestion.validation;

import org.springframework.stereotype.Component;

import com.smartlog.common.enums.LogLevel;
import com.smartlog.ingestion.dto.LogIngestionRequest;

@Component
public class LogValidator {

    public void validate(LogIngestionRequest request) {
        try {
            LogLevel.from(request.level());
        } catch (IllegalArgumentException exception) {
            throw new InvalidLogRequestException(exception.getMessage());
        }
    }
}
