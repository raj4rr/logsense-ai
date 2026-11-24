package com.logsense.audit;
import java.util.Map;
/** Replace with a SecureMCP audit adapter to reuse the shared audit pipeline. */
public interface AuditSink { void write(String actor, String action, String resource, String outcome, Map<String, ?> details); }
