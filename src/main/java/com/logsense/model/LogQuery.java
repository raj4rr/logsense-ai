package com.logsense.model;

import java.time.Instant;

public record LogQuery(String source, String keyword, String severity, String service,
                       Instant from, Instant to, String traceId, Integer limit) {}
