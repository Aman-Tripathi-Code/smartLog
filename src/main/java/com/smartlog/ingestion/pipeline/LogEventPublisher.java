package com.smartlog.ingestion.pipeline;

import java.util.List;

import com.smartlog.common.model.LogEvent;

public interface LogEventPublisher {

    void publish(LogEvent event);

    void publishAll(List<LogEvent> events);
}
