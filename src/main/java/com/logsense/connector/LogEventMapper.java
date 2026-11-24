package com.logsense.connector;

import com.fasterxml.jackson.databind.*;
import com.logsense.model.LogEvent;

import java.time.*;
import java.util.*;
import java.util.regex.*;

public final class LogEventMapper {
    private static final Pattern LEVEL = Pattern.compile("\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL)\\b", Pattern.CASE_INSENSITIVE);

    private LogEventMapper() {
    }

    public static LogEvent fromText(String source, String text, Instant fallback, Map<String, Object> attrs) {
        String s = text == null ? "" : text;
        String severity = "INFO";
        Matcher m = LEVEL.matcher(s);
        if (m.find()) severity = m.group(1).equalsIgnoreCase("WARNING") ? "WARN" : m.group(1).toUpperCase(Locale.ROOT);
        Instant ts = fallback == null ? Instant.now() : fallback;
        Matcher tm = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2}))").matcher(s);
        if (tm.find()) try {
            ts = Instant.parse(tm.group(1));
        } catch (Exception ignored) {
        }
        String service = null;
        Matcher sm = Pattern.compile("\\b(?:TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL)\\s+([A-Za-z0-9._-]+)", Pattern.CASE_INSENSITIVE).matcher(s);
        if (sm.find()) service = sm.group(1);
        return new LogEvent(source, service, null, severity, ts, s, null, extract(s, "traceId|trace_id"), extract(s, "spanId|span_id"), extract(s, "requestId|request_id"), null, attrs);
    }

    public static LogEvent fromJson(String source, JsonNode n, Instant fallback) {
        String msg = text(n, "message", text(n, "msg", n.toString()));
        String level = text(n, "severity", text(n, "level", "INFO"));
        Instant ts = fallback;
        String raw = text(n, "@timestamp", text(n, "timestamp", null));
        if (raw != null) try {
            ts = Instant.parse(raw);
        } catch (Exception ignored) {
        }
        Map<String, Object> a = new LinkedHashMap<>();
        n.fields().forEachRemaining(e -> {
            if (!Set.of("message", "msg", "severity", "level", "@timestamp", "timestamp").contains(e.getKey()))
                a.put(e.getKey(), e.getValue().asText());
        });
        String service = n.path("service").isObject() ? text(n.path("service"), "name", null) : text(n, "service", null);
        String host = n.path("host").isObject() ? text(n.path("host"), "name", null) : text(n, "host", null);
        return new LogEvent(source, service, text(n, "environment", null), level, ts, msg, text(n, "logger", null), text(n, "traceId", text(n, "trace_id", null)), text(n, "spanId", null), text(n, "requestId", null), host, a);
    }

    private static String text(JsonNode n, String key, String d) {
        JsonNode v = n.path(key);
        return v.isMissingNode() || v.isNull() ? d : v.asText();
    }

    private static String extract(String s, String keys) {
        Matcher m = Pattern.compile("(?:" + keys + ")\\s*[=:]\\s*([A-Za-z0-9._-]+)", Pattern.CASE_INSENSITIVE).matcher(s);
        return m.find() ? m.group(1) : null;
    }
}
