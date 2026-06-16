package com.smartlog.processing.consumer;

public interface DeadLetterRepository {

    DeadLetterRecord save(DeadLetterRecord record);
}
