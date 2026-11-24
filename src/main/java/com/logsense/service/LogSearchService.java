package com.logsense.service;

import com.logsense.connector.ConnectorRegistry;
import com.logsense.model.LogEvent;
import com.logsense.model.LogQuery;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LogSearchService {
    private final ConnectorRegistry registry;
    public LogSearchService(ConnectorRegistry registry) { this.registry = registry; }
    public List<LogEvent> search(String type, String source, String keyword, String severity, String service, String traceId, Integer limit) {
        return registry.require(type).search(source, new LogQuery(source, keyword, severity, service, null, null, traceId, limit));
    }
}
