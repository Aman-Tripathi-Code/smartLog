package com.smartlog.ingestion.pipeline;

public class LogQueueFullException extends RuntimeException {

    public LogQueueFullException(String message) {
        super(message);
    }
}
