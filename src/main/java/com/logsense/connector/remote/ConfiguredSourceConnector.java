package com.logsense.connector.remote;

import com.logsense.connector.LogConnector;
import com.logsense.model.LogEvent;
import com.logsense.model.LogQuery;
import org.springframework.stereotype.Component;
import java.util.List;

/** Explicit placeholder: remote adapters are registered by type but fail closed until endpoint/auth wiring is supplied. */
@Component
public class ConfiguredSourceConnector implements LogConnector {
    private static final List<String> TYPES = List.of("DOCKER","KUBERNETES","ELASTICSEARCH","OPENSEARCH","CLOUDWATCH","LOKI","SPLUNK","POSTGRESQL","KAFKA");
    public String type() { return "REMOTE_ADAPTERS"; }
    public boolean supports(String t) { return TYPES.contains(t.toUpperCase()); }
    public List<LogEvent> search(String sourceName, LogQuery query) {
        throw new UnsupportedOperationException("Connector '"+sourceName+"' is scaffolded but not configured in this MVP build. See docs/CONNECTORS.md.");
    }
}
