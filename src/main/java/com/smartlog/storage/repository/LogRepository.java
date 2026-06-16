package com.smartlog.storage.repository;

import java.util.List;

import com.smartlog.common.model.LogEvent;
import com.smartlog.query.dto.LogSearchCriteria;
import com.smartlog.query.dto.LogSearchPage;
import com.smartlog.query.dto.LogSearchResult;

public interface LogRepository {

    void save(LogEvent event);

    void saveAll(List<LogEvent> events);

    LogSearchPage<LogSearchResult> search(LogSearchCriteria criteria);
}
