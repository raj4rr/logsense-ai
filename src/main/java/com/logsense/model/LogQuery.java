package com.logsense.model;
import java.time.Instant;
public record LogQuery(String source, String keyword, String severity, String service, Instant from, Instant to, String traceId, Integer limit) {
 public int safeLimit() { return Math.max(1, Math.min(limit == null ? 100 : limit, 500)); }
}
