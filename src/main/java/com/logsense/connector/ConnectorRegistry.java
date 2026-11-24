package com.logsense.connector;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConnectorRegistry {
    private final List<LogConnector> connectors;

    public ConnectorRegistry(List<LogConnector> connectors) {
        this.connectors = List.copyOf(connectors);
    }

    public LogConnector require(String type) {
        return connectors.stream().filter(c -> c.supports(type)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No connector implementation for type: " + type));
    }

    public List<String> supportedTypes() {
        return connectors.stream().flatMap(c -> c.supportedTypes().stream()).distinct().sorted().toList();
    }
}
