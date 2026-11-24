package com.logsense.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuditService {
    private final AuditSink sink;

    public AuditService(AuditSink sink) {
        this.sink = sink;
    }

    public void record(String action, String resource, String outcome, Map<String, ?> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "anonymous" : auth.getName();
        Map<String, Object> enriched = new LinkedHashMap<>();
        if (details != null) enriched.putAll(details);
        if (auth instanceof JwtAuthenticationToken jwt) {
            String tenant = jwt.getToken().getClaimAsString("tenant_id");
            if (tenant != null) enriched.put("tenantId", tenant);
        }
        sink.write(actor, action, resource, outcome, enriched);
    }
}
