package com.logsense.connector.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logsense.config.LogSenseProperties;
import com.logsense.connector.LogConnector;
import com.logsense.model.LogEvent;
import com.logsense.model.LogQuery;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

@Component
public class LocalFileConnector implements LogConnector {
    private static final Pattern LEVEL = Pattern.compile("\b(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISO = Pattern.compile("(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2}))");
    private final List<Path> roots;
    public LocalFileConnector(LogSenseProperties props) {
        this.roots = (props.files() == null || props.files().allowedRoots() == null ? List.of("./sample-logs") : props.files().allowedRoots())
            .stream().map(p -> Path.of(p).toAbsolutePath().normalize()).toList();
    }
    public String type() { return "FILE"; }
    public boolean supports(String t) { return "FILE".equalsIgnoreCase(t) || "LOCAL_FILE".equalsIgnoreCase(t); }
    public List<LogEvent> search(String sourceName, LogQuery q) {
        Path requested = Path.of(sourceName).toAbsolutePath().normalize();
        if (roots.stream().noneMatch(requested::startsWith)) throw new SecurityException("File path is outside configured allowed roots");
        if (!Files.isRegularFile(requested) || !Files.isReadable(requested)) throw new IllegalArgumentException("Log file not found or unreadable");
        int limit = q.limit() == null ? 100 : Math.min(Math.max(q.limit(),1),500);
        List<LogEvent> out = new ArrayList<>();
        try (Stream<String> lines = Files.lines(requested)) {
            lines.forEach(line -> {
                if (out.size() >= limit) return;
                String upper = line.toUpperCase(Locale.ROOT);
                Matcher lm = LEVEL.matcher(line); String severity = lm.find() ? lm.group(1).toUpperCase(Locale.ROOT) : "INFO";
                Matcher tm = ISO.matcher(line); Instant ts = tm.find() ? parse(tm.group(1)) : Instant.now();
                if (q.keyword()!=null && !line.toLowerCase(Locale.ROOT).contains(q.keyword().toLowerCase(Locale.ROOT))) return;
                if (q.severity()!=null && !severity.equalsIgnoreCase(q.severity())) return;
                if (q.from()!=null && ts.isBefore(q.from())) return;
                if (q.to()!=null && ts.isAfter(q.to())) return;
                out.add(new LogEvent("file", null, null, severity, ts, line, null, null, null, null, requested.getFileName().toString(), Map.of("path", requested.toString())));
            });
        } catch (IOException e) { throw new IllegalStateException("Unable to read log file", e); }
        return out;
    }
    private Instant parse(String value) { try { return Instant.parse(value); } catch (Exception e) { return Instant.now(); } }
}
