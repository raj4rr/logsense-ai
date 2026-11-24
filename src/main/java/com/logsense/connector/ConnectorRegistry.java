package com.logsense.connector;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ConnectorRegistry {
    private final List<LogConnector> connectors;
    public ConnectorRegistry(List<LogConnector> connectors) { this.connectors = connectors; }
    public LogConnector require(String type) {
        return connectors.stream().filter(c -> c.supports(type)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enabled connector implementation for type: " + type));
    }
    public List<String> supportedTypes() { return connectors.stream().map(LogConnector::type).toList(); }
}
