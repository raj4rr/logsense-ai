package com.logsense.mcp;

import com.logsense.analysis.IncidentCorrelationService;
import com.logsense.analysis.LogAnalysisService;
import com.logsense.model.LogEvent;
import com.logsense.service.LogSearchService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class LogMcpTools {
    private final LogSearchService search;
    private final LogAnalysisService analysis;
    private final IncidentCorrelationService correlation;
    public LogMcpTools(LogSearchService search, LogAnalysisService analysis, IncidentCorrelationService correlation) {
        this.search = search; this.analysis = analysis; this.correlation = correlation;
    }
    @McpTool(name="list_log_sources", description="List implemented read-only log connector types")
    public Map<String,Object> listLogSources() { return Map.of("connectors", search.supportedTypes(), "access", "read-only"); }

    @McpTool(name="search_logs", description="Search a configured log source by keyword, severity, service and trace identifier")
    public List<LogEvent> searchLogs(@McpToolParam(description="Connector type: FILE, DOCKER, KUBERNETES, ELASTICSEARCH, OPENSEARCH, CLOUDWATCH, LOKI, SPLUNK, POSTGRESQL or KAFKA", required=true) String type,
            @McpToolParam(description="File path, index, log group, namespace, topic, container id or Loki selector", required=true) String source,
            @McpToolParam(description="Optional keyword") String keyword, @McpToolParam(description="Optional severity") String severity,
            @McpToolParam(description="Optional service") String service, @McpToolParam(description="Optional trace id") String traceId,
            @McpToolParam(description="Maximum results 1-500") Integer limit) {
        return search.search(type, source, keyword, severity, service, traceId, limit);
    }
    @McpTool(name="get_error_logs", description="Retrieve ERROR/FATAL/CRITICAL log entries from a source")
    public List<LogEvent> getErrorLogs(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Maximum results") Integer limit) {
        List<LogEvent> rows = search.search(type, source, null, null, null, null, limit);
        return rows.stream().filter(e -> Set.of("ERROR", "FATAL", "CRITICAL").contains(e.severity())).toList();
    }
    @McpTool(name="analyze_logs", description="Analyze log evidence and provide likely causes, confidence, diagnostics and safe remediation suggestions")
    public Map<String,Object> analyzeLogs(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Question", required=true) String question,
            @McpToolParam(description="Optional keyword") String keyword, @McpToolParam(description="Optional severity") String severity,
            @McpToolParam(description="Maximum results") Integer limit) {
        return analysis.analyze(search.search(type, source, keyword, severity, null, null, limit), question);
    }
    @McpTool(name="find_root_cause", description="Investigate likely root causes from bounded log evidence; results are hypotheses with evidence")
    public Map<String,Object> findRootCause(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Incident description", required=true) String incident,
            @McpToolParam(description="Maximum results") Integer limit) {
        return analysis.analyze(search.search(type, source, null, null, null, null, limit), "Find likely root causes for: " + incident);
    }
    @McpTool(name="correlate_logs", description="Group events by shared trace ID and request ID and return ordered timelines")
    public Map<String,Object> correlateLogs(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Trace ID or request ID to search", required=true) String correlationId,
            @McpToolParam(description="Maximum results") Integer limit) {
        List<LogEvent> rows = search.search(type, source, null, null, null, correlationId, limit);
        return correlation.correlate(rows);
    }
    @McpTool(name="detect_anomalies", description="Identify unusual severity concentration and repeated error signatures in the selected log sample")
    public Map<String,Object> detectAnomalies(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Maximum results") Integer limit) {
        List<LogEvent> rows = search.search(type, source, null, null, null, null, limit);
        long errors = rows.stream().filter(e -> Set.of("ERROR", "FATAL", "CRITICAL").contains(e.severity())).count();
        Map<String,Long> signatures = rows.stream().filter(e -> Set.of("ERROR", "FATAL", "CRITICAL").contains(e.severity()))
                .collect(java.util.stream.Collectors.groupingBy(e -> e.message().replaceAll("[0-9]+", "<n>"), java.util.stream.Collectors.counting()));
        List<Map<String,Object>> repeated = signatures.entrySet().stream().filter(e -> e.getValue() >= 3)
                .map(e -> Map.<String,Object>of("signature", e.getKey(), "count", e.getValue())).toList();
        return Map.of("sampleSize", rows.size(), "errorCount", errors, "errorRatio", rows.isEmpty()?0.0:(double)errors/rows.size(),
                "repeatedErrorSignatures", repeated, "caveat", "Heuristic sample-based detection; requires historical baselines for statistically reliable anomaly detection.");
    }
    @McpTool(name="generate_incident_report", description="Generate an evidence-backed incident summary and next diagnostic steps")
    public Map<String,Object> generateIncidentReport(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Incident title/question", required=true) String incident,
            @McpToolParam(description="Maximum results") Integer limit) {
        return analysis.analyze(search.search(type, source, null, null, null, null, limit), "Create an incident report for: " + incident);
    }

    @McpTool(name="analyze_performance", description="Find slow-operation evidence in logs using duration fields or duration-like message patterns")
    public Map<String,Object> analyzePerformance(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Maximum results") Integer limit) {
        List<LogEvent> rows = search.search(type, source, null, null, null, null, limit);
        List<Map<String,Object>> slow = rows.stream().filter(e -> e.message().matches("(?is).*(duration|elapsed|took)[=: ]+([1-9][0-9]{3,})\\s*(ms|milliseconds)?.*"))
                .limit(100).map(e -> Map.<String,Object>of("timestamp",e.timestamp(),"service",e.service()==null?"unknown":e.service(),"severity",e.severity(),"message",e.message())).toList();
        return Map.of("sampleSize",rows.size(),"slowOperationCandidates",slow,"note","Duration parsing is heuristic. Confirm units and request traces before drawing conclusions.");
    }
    @McpTool(name="get_service_health", description="Summarize severity distribution and latest observed errors for a log source")
    public Map<String,Object> getServiceHealth(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Maximum results") Integer limit) {
        List<LogEvent> rows = search.search(type, source, null, null, null, null, limit);
        Map<String,Long> counts = rows.stream().collect(java.util.stream.Collectors.groupingBy(LogEvent::severity, java.util.stream.Collectors.counting()));
        List<LogEvent> latestErrors = rows.stream().filter(e -> Set.of("ERROR","FATAL","CRITICAL").contains(e.severity())).sorted(Comparator.comparing(LogEvent::timestamp).reversed()).limit(10).toList();
        return Map.of("sampleSize",rows.size(),"severityCounts",counts,"latestErrors",latestErrors,"healthAssessment",latestErrors.isEmpty()?"No errors found in sampled logs":"Errors present in sampled logs","caveat","This is a log-sample summary, not a complete service availability SLI/SLO.");
    }
    @McpTool(name="suggest_fixes", description="Suggest safe, non-executing remediation ideas grounded in retrieved log evidence")
    public Map<String,Object> suggestFixes(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Describe the problem", required=true) String problem,
            @McpToolParam(description="Maximum results") Integer limit) {
        return analysis.analyze(search.search(type, source, null, null, null, null, limit), "Suggest safe fixes for: " + problem + ". Cite evidence and distinguish hypotheses from confirmed facts. Do not execute changes.");
    }

    @McpTool(name="find_trace_logs", description="Find events with a distributed trace identifier in a selected source")
    public List<LogEvent> findTraceLogs(@McpToolParam(description="Connector type", required=true) String type,
            @McpToolParam(description="Source identifier", required=true) String source,
            @McpToolParam(description="Trace identifier", required=true) String traceId,
            @McpToolParam(description="Maximum results") Integer limit) {
        return search.search(type, source, null, null, null, traceId, limit);
    }
}
