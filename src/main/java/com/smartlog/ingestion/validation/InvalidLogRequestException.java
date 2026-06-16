package com.smartlog.ingestion.validation;

public class InvalidLogRequestException extends RuntimeException {

    public InvalidLogRequestException(String message) {
        super(message);
    }
}
