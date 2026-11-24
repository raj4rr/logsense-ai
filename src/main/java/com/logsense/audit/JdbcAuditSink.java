package com.logsense.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JdbcAuditSink implements AuditSink {
    private static final Logger log = LoggerFactory.getLogger(JdbcAuditSink.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcAuditSink(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public void write(String actor, String action, String resource, String outcome, Map<String, ?> details) {
        try {
            jdbc.update("insert into audit_event(actor,action,resource,outcome,details) values (?,?,?,?,?::jsonb)", 
                    actor, action, resource, outcome, mapper.writeValueAsString(details == null ? Map.of() : details));
        } catch (Exception e) {
            log.warn("Audit database persistence failed (recording audit to logs instead): actor={}, action={}, resource={}, outcome={}, error={}",
                    actor, action, resource, outcome, e.getMessage());
        }
    }
}
