package com.logsense.connector.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.logsense.config.LogSenseProperties;
import com.logsense.connector.LogConnector;
import com.logsense.connector.LogEventMapper;
import com.logsense.connector.file.LocalFileConnector;
import com.logsense.model.LogEvent;
import com.logsense.model.LogQuery;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
public class ElasticsearchConnector implements LogConnector {
    private final LogSenseProperties properties;

    public ElasticsearchConnector(LogSenseProperties properties) {
        this.properties = properties;
    }

    @Override
    public String type() {
        return "ELASTICSEARCH";
    }

    @Override
    public Set<String> supportedTypes() {
        return Set.of("ELASTICSEARCH", "OPENSEARCH");
    }

    @Override
    public boolean supports(String type) {
        return Set.of("ELASTICSEARCH", "OPENSEARCH").contains(type.toUpperCase(Locale.ROOT));
    }

    @Override
    public List<LogEvent> search(String index, LogQuery query) {
        var config = properties.sources().elasticsearch();
        if (config == null || config.url() == null || config.url().isBlank())
            throw new IllegalStateException("Set ELASTICSEARCH_URL");
        if (index == null || !index.matches("[A-Za-z0-9_.*,-]{1,200}"))
            throw new SecurityException("Invalid Elasticsearch index/alias");
        StringBuilder expression = new StringBuilder("*");
        if (query.keyword() != null)
            expression.append(" AND message:").append(query.keyword().replaceAll("[^A-Za-z0-9._ -]", " "));
        if (query.severity() != null)
            expression.append(" AND severity:").append(query.severity().replaceAll("[^A-Za-z]", ""));
        if (query.service() != null)
            expression.append(" AND service:").append(query.service().replaceAll("[^A-Za-z0-9._-]", ""));
        if (query.from() != null) expression.append(" AND @timestamp:>=").append(query.from());
        if (query.to() != null) expression.append(" AND @timestamp:<=").append(query.to());
        String url = config.url().replaceAll("/$", "") + "/" + HttpConnectorSupport.enc(index)
                + "/_search?size=" + query.safeLimit() + "&sort=@timestamp:desc&q=" + HttpConnectorSupport.enc(expression.toString());
        Map<String, String> headers;
        if (config.token() != null && !config.token().isBlank()) headers = HttpConnectorSupport.bearer(config.token());
        else if (config.username() != null && !config.username().isBlank()) {
            String raw = config.username() + ":" + (config.password() == null ? "" : config.password());
            headers = Map.of("Accept", "application/json", "Authorization", "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
        } else headers = Map.of("Accept", "application/json");
        JsonNode response = HttpConnectorSupport.get(url, headers);
        List<LogEvent> events = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            LogEvent event = LogEventMapper.fromJson(index, hit.path("_source"), Instant.now());
            if (LocalFileConnector.matches(event, query)) events.add(event);
        }
        return events;
    }
}
