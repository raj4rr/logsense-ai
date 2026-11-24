package com.logsense.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logsense.model.LogEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.*;

@Component
public class LogQueryCache {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public LogQueryCache(StringRedisTemplate redis, ObjectMapper mapper) { this.redis = redis; this.mapper = mapper; }

    public List<LogEvent> get(String key) {
        try {
            Future<String> future = executor.submit(() -> redis.opsForValue().get(key));
            String value = future.get(500, TimeUnit.MILLISECONDS);
            return value == null ? null : mapper.readValue(value, new TypeReference<List<LogEvent>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    public void put(String key, List<LogEvent> events) {
        try {
            executor.submit(() -> {
                try {
                    redis.opsForValue().set(key, mapper.writeValueAsString(events), Duration.ofSeconds(5));
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {
            /* Cache is best-effort; source reads remain authoritative. */
        }
    }

    public static String key(String type, String source, String keyword, String severity, String service,
                             String traceId, Object from, Object to, int limit) {
        String raw = String.join("|", safe(type), safe(source), safe(keyword), safe(severity), safe(service), safe(traceId),
                String.valueOf(from), String.valueOf(to), String.valueOf(limit));
        try { return "logsense:q:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static String safe(String s) { return s == null ? "" : s; }
}
