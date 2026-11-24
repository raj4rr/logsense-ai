package com.logsense.model;

import java.time.Instant;
import java.util.Map;

public record LogEvent(String source, String service, String environment, String severity,
                       Instant timestamp, String message, String logger, String traceId,
                       String spanId, String requestId, String host, Map<String,Object> attributes) {}
