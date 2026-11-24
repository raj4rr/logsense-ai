package com.logsense.analysis;

import com.logsense.model.LogEvent;
import com.logsense.analysis.LogRedactor;
import org.springframework.stereotype.Service;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncidentCorrelationService {
    private final LogRedactor redactor;
    public IncidentCorrelationService(LogRedactor redactor) { this.redactor = redactor; }
    public Map<String, Object> correlate(List<LogEvent> events) {
        Map<String, List<LogEvent>> traces = events.stream().filter(e -> e.traceId() != null && !e.traceId().isBlank())
                .collect(Collectors.groupingBy(LogEvent::traceId));
        Map<String, List<LogEvent>> requests = events.stream().filter(e -> e.requestId() != null && !e.requestId().isBlank())
                .collect(Collectors.groupingBy(LogEvent::requestId));
        List<Map<String, Object>> chains = new ArrayList<>();
        traces.forEach((id, rows) -> { if (rows.size() > 1) chains.add(chain("traceId", id, rows)); });
        requests.forEach((id, rows) -> { if (rows.size() > 1) chains.add(chain("requestId", id, rows)); });
        return Map.of("correlationGroups", chains, "eventsWithTraceId", traces.values().stream().mapToInt(List::size).sum(),
                "eventsWithRequestId", requests.values().stream().mapToInt(List::size).sum(),
                "note", "Correlation is identifier-based; temporal proximity alone does not prove causality.");
    }
    private Map<String, Object> chain(String key, String id, List<LogEvent> rows) {
        List<LogEvent> ordered = rows.stream().sorted(Comparator.comparing(LogEvent::timestamp)).toList();
        List<Map<String, Object>> timeline = ordered.stream().map(e -> Map.<String, Object>of(
                "timestamp", e.timestamp(), "source", e.source(), "service", e.service() == null ? "unknown" : e.service(),
                "severity", e.severity(), "message", redactor.redact(e.message()).length() > 800 ? redactor.redact(e.message()).substring(0, 800) : redactor.redact(e.message()))).toList();
        return Map.of("identifierType", key, "identifier", id, "eventCount", rows.size(), "timeline", timeline,
                "firstError", ordered.stream().filter(e -> Set.of("ERROR", "FATAL", "CRITICAL").contains(e.severity())).map(e -> e.timestamp().toString()).findFirst().orElse("none"));
    }
}
