package com.logsense.connector.remote;

import com.logsense.config.LogSenseProperties;
import com.logsense.connector.*;
import com.logsense.connector.file.LocalFileConnector;
import com.logsense.model.*;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.*;

import java.time.*;
import java.util.*;

@Component
public class SplunkConnector implements LogConnector {
    private final LogSenseProperties p;

    public SplunkConnector(LogSenseProperties p) {
        this.p = p;
    }

    public String type() {
        return "SPLUNK";
    }

    public boolean supports(String t) {
        return "SPLUNK".equalsIgnoreCase(t);
    }

    public List<LogEvent> search(String index, LogQuery q) {
        var c = p.sources().splunk();
        if (c == null || c.url() == null || c.url().isBlank() || c.token() == null || c.token().isBlank())
            throw new IllegalStateException("Configure SPLUNK_URL and SPLUNK_TOKEN");
        String selected = index == null || index.isBlank() ? c.index() : index;
        if (selected == null || !selected.matches("[A-Za-z0-9_.-]{1,100}"))
            throw new SecurityException("Splunk source must be a valid index name; arbitrary SPL is disabled");
        StringBuilder spl = new StringBuilder("search index=\"").append(selected).append("\"");
        if (q.keyword() != null) spl.append(" \"").append(q.keyword().replace("\"", " ")).append("\"");
        if (q.severity() != null)
            spl.append(" severity=\"").append(q.severity().replaceAll("[^A-Za-z]", "")).append("\"");
        if (q.from() != null) spl.append(" earliest=").append(q.from().getEpochSecond());
        if (q.to() != null) spl.append(" latest=").append(q.to().getEpochSecond());
        String endpoint = c.url().replaceAll("/$", "") + "/services/search/jobs/export";
        String body = "output_mode=json&search=" + HttpConnectorSupport.enc(spl + " | head " + q.safeLimit());
        String raw = HttpConnectorSupport.post(endpoint, body, HttpConnectorSupport.bearer(c.token()));
        List<LogEvent> out = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            if (line.isBlank()) continue;
            try {
                JsonNode n = HttpConnectorSupport.M.readTree(line);
                JsonNode r = n.has("result") ? n.path("result") : n;
                LogEvent e = LogEventMapper.fromJson("splunk:" + selected, r, Instant.now());
                if (LocalFileConnector.matches(e, q)) out.add(e);
            } catch (Exception ignored) {/* Splunk may emit job metadata lines; skip non-event rows. */}
        }
        return out;
    }
}
