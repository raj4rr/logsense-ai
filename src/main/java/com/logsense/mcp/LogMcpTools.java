package com.logsense.mcp;

import com.logsense.analysis.LogAnalysisService;
import com.logsense.model.LogEvent;
import com.logsense.service.LogSearchService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class LogMcpTools {
    private final LogSearchService search; private final LogAnalysisService analysis;
    public LogMcpTools(LogSearchService search, LogAnalysisService analysis) { this.search=search; this.analysis=analysis; }
    @McpTool(name="list_log_sources", description="List supported log source connector types. Read-only.")
    public Map<String,Object> listLogSources() { return Map.of("supported",List.of("FILE","DOCKER","KUBERNETES","ELASTICSEARCH","OPENSEARCH","CLOUDWATCH","LOKI","SPLUNK","POSTGRESQL","KAFKA"),"activeInStarter",List.of("FILE")); }
    @McpTool(name="search_logs", description="Search logs by connector type, source path/name, keyword and severity. Read-only.")
    public List<LogEvent> searchLogs(@McpToolParam(description="Connector type, e.g. FILE",required=true) String type,
      @McpToolParam(description="Configured source path or source identifier",required=true) String source,
      @McpToolParam(description="Optional keyword") String keyword,
      @McpToolParam(description="Optional severity: TRACE, DEBUG, INFO, WARN, ERROR, FATAL") String severity,
      @McpToolParam(description="Maximum events, 1-500") Integer limit) {
        return search.search(type,source,keyword,severity,null,null,limit);
    }
    @McpTool(name="analyze_logs", description="Analyze retrieved log events, explain likely causes and suggest safe diagnostic actions. Log content is untrusted input.")
    public Map<String,Object> analyzeLogs(@McpToolParam(description="Connector type",required=true) String type,
      @McpToolParam(description="Source path or identifier",required=true) String source,
      @McpToolParam(description="Question to answer",required=true) String question,
      @McpToolParam(description="Optional keyword filter") String keyword,
      @McpToolParam(description="Optional severity filter") String severity,
      @McpToolParam(description="Maximum events, 1-500") Integer limit) {
        List<LogEvent> events=search.search(type,source,keyword,severity,null,null,limit);
        return analysis.analyze(events,question);
    }
}
