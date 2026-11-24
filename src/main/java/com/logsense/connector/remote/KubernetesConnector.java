package com.logsense.connector.remote;

import com.logsense.config.LogSenseProperties;
import com.logsense.connector.*;
import com.logsense.connector.file.LocalFileConnector;
import com.logsense.model.*;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.*;

import java.util.*;
import java.time.*;

@Component
public class KubernetesConnector implements LogConnector {
    private final LogSenseProperties p;

    public KubernetesConnector(LogSenseProperties p) {
        this.p = p;
    }

    public String type() {
        return "KUBERNETES";
    }

    public boolean supports(String t) {
        return "KUBERNETES".equalsIgnoreCase(t);
    }

    public List<LogEvent> search(String target, LogQuery q) {
        var c = p.sources().kubernetes();
        if (c == null || !c.enabled() || c.apiUrl() == null || c.apiUrl().isBlank() || c.bearerToken() == null || c.bearerToken().isBlank())
            throw new IllegalStateException("Set KUBERNETES_ENABLED=true, KUBERNETES_API_URL and KUBERNETES_TOKEN");
        String ns = target == null || target.isBlank() ? c.namespace() : target;
        if (ns == null || !ns.matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?"))
            throw new SecurityException("Invalid Kubernetes namespace");
        String base = c.apiUrl().replaceAll("/$", "");
        String url = base + "/api/v1/namespaces/" + HttpConnectorSupport.enc(ns) + "/pods?limit=" + Math.min(q.safeLimit(), 100);
        Map<String, String> h = HttpConnectorSupport.bearer(c.bearerToken());
        JsonNode pods = HttpConnectorSupport.get(url, h);
        List<LogEvent> out = new ArrayList<>();
        for (JsonNode pod : pods.path("items")) {
            String name = pod.path("metadata").path("name").asText();
            for (JsonNode container : pod.path("spec").path("containers")) {
                if (out.size() >= q.safeLimit()) break;
                String cn = container.path("name").asText();
                String logUrl = base + "/api/v1/namespaces/" + HttpConnectorSupport.enc(ns) + "/pods/" + HttpConnectorSupport.enc(name) + "/log?container=" + HttpConnectorSupport.enc(cn) + "&tailLines=" + Math.min(q.safeLimit(), 2000);
                try {
                    String body = HttpConnectorSupport.getText(logUrl + "&timestamps=true", h);
                    for (String line : body.split("\n")) {
                        LogEvent e = LogEventMapper.fromText("kubernetes:" + ns + "/" + name, line, Instant.now(), Map.of("pod", name, "container", cn));
                        if (LocalFileConnector.matches(e, q) && out.size() < q.safeLimit()) out.add(e);
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException("Kubernetes log retrieval failed for pod " + name, ex);
                }
            }
        }
        return out;
    }
}
