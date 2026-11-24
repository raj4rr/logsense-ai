package com.logsense.service;

import com.logsense.audit.AuditService;
import com.logsense.connector.ConnectorRegistry;
import com.logsense.model.LogEvent;
import com.logsense.model.LogQuery;
import com.logsense.config.LogSenseProperties;
import com.logsense.security.SourceAuthorizationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class LogSearchService {
    private final ConnectorRegistry registry;
    private final SourceAuthorizationService authorization;
    private final AuditService audit;
    private final LogQueryCache cache;
    private final LogSenseProperties properties;

    public LogSearchService(ConnectorRegistry registry, SourceAuthorizationService authorization, AuditService audit, LogQueryCache cache, LogSenseProperties properties) {
        this.registry = registry;
        this.authorization = authorization;
        this.audit = audit;
        this.cache = cache;
        this.properties = properties;
    }

    public List<LogEvent> search(String type, String source, String keyword, String severity, String service, String traceId, Integer limit) {
        return search(type, source, keyword, severity, service, traceId, null, null, limit);
    }

    public List<LogEvent> search(String type, String source, String keyword, String severity, String service,
                                 String traceId, Instant from, Instant to, Integer limit) {
        try {
            authorization.requireReadAccess(type, source);
        } catch (RuntimeException denied) {
            audit.record("LOG_SEARCH", String.valueOf(type) + ":" + source, "DENIED", Map.of("error", denied.getClass().getSimpleName()));
            throw denied;
        }
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("from must be before to");
        int maxResults = properties.limits() == null || properties.limits().maxResults() == null ? 500 : Math.max(1, Math.min(500, properties.limits().maxResults()));
        int maxHours = properties.limits() == null || properties.limits().maxWindowHours() == null ? 168 : Math.max(1, Math.min(168, properties.limits().maxWindowHours()));
        Instant now = Instant.now();
        Instant effectiveTo = to == null ? Instant.ofEpochSecond((now.getEpochSecond() / 5) * 5) : to;
        Instant effectiveFrom = from == null ? effectiveTo.minusSeconds(maxHours * 3600L) : from;
        if (effectiveFrom.isAfter(effectiveTo)) throw new IllegalArgumentException("from must not be after to");
        if (effectiveFrom.isBefore(effectiveTo.minusSeconds(maxHours * 3600L)))
            throw new IllegalArgumentException("Requested time window exceeds configured maximum of " + maxHours + " hours");
        int safeLimit = Math.max(1, Math.min(limit == null ? 100 : limit, maxResults));
        LogQuery q = new LogQuery(source, keyword, severity, service, effectiveFrom, effectiveTo, traceId, safeLimit);
        try {
            String cacheKey = LogQueryCache.key(type, source, keyword, severity, service, traceId, effectiveFrom, effectiveTo, q.safeLimit());
            List<LogEvent> events = cache.get(cacheKey);
            boolean cacheHit = events != null;
            if (!cacheHit) {
                events = registry.require(type).search(source, q);
                cache.put(cacheKey, events);
            }
            audit.record("LOG_SEARCH", type + ":" + source, "SUCCESS",
                    Map.of("count", events.size(), "limit", q.safeLimit(), "severity", severity == null ? "" : severity,
                            "keywordPresent", keyword != null, "traceFilterPresent", traceId != null, "cacheHit", cacheHit));
            return events;
        } catch (RuntimeException e) {
            audit.record("LOG_SEARCH", type + ":" + source, "FAILURE", Map.of("error", e.getClass().getSimpleName()));
            throw e;
        }
    }

    public List<String> supportedTypes() {
        return registry.supportedTypes();
    }
}
