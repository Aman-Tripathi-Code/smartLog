package com.smartlog.storage.repository;

import java.util.List;

import com.smartlog.common.model.LogEvent;

public interface LogRepository {

    void save(LogEvent event);

    void saveAll(List<LogEvent> events);
}
