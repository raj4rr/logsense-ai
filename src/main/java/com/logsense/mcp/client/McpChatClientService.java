package com.logsense.mcp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logsense.analysis.LogAnalysisService;
import com.logsense.config.LogSenseProperties;
import com.logsense.model.LogEvent;
import com.logsense.service.LogSearchService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class McpChatClientService {
    private static final Logger log = LoggerFactory.getLogger(McpChatClientService.class);

    private final LogSearchService searchService;
    private final LogAnalysisService analysisService;
    private final ChatClient chatClient;
    private final LogSenseProperties properties;
    private final ObjectMapper objectMapper;

    @Value("${server.port:8080}")
    private int serverPort;

    private McpSyncClient mcpSyncClient;
    private final List<McpSchema.Tool> registeredTools = new ArrayList<>();

    public record PromptSuggestion(String id, String title, String prompt, String badge, String category) {}

    public record ToolInfo(String name, String description, Map<String, Object> inputSchema) {}

    public record ChatResponse(String answer, String toolUsed, Object toolOutput, List<PromptSuggestion> nextSuggestions) {}

    public McpChatClientService(LogSearchService searchService,
                                LogAnalysisService analysisService,
                                ChatClient chatClient,
                                LogSenseProperties properties,
                                ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.analysisService = analysisService;
        this.chatClient = chatClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            String baseUrl = "http://localhost:" + serverPort;
            String apiKey = properties.security() != null ? properties.security().apiKey() : null;

            var transportBuilder = HttpClientStreamableHttpTransport.builder(baseUrl)
                    .endpoint("/mcp")
                    .connectTimeout(Duration.ofSeconds(3));

            if (apiKey != null && !apiKey.isBlank()) {
                transportBuilder.httpRequestCustomizer((builder, method, uri, body, ctx) -> builder.header("X-API-Key", apiKey));
            }

            mcpSyncClient = McpClient.sync(transportBuilder.build())
                    .requestTimeout(Duration.ofSeconds(10))
                    .build();

            mcpSyncClient.initialize();
            var listResult = mcpSyncClient.listTools();
            if (listResult != null && listResult.tools() != null) {
                registeredTools.addAll(listResult.tools());
            }
            log.info("MCP Client connected to {}/mcp successfully with {} tools", baseUrl, registeredTools.size());
        } catch (Exception e) {
            log.warn("MCP Client background connection to local server will use direct fallback: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        if (mcpSyncClient != null) {
            try {
                mcpSyncClient.closeGracefully();
            } catch (Exception ignored) {}
        }
    }

    public List<ToolInfo> getAvailableTools() {
        if (!registeredTools.isEmpty()) {
            return registeredTools.stream()
                    .map(t -> new ToolInfo(t.name(), t.description(), t.inputSchema()))
                    .toList();
        }
        return List.of(
                new ToolInfo("list_log_sources", "List implemented read-only log connector types", Map.of()),
                new ToolInfo("search_logs", "Search a configured log source by keyword, severity, service and trace", Map.of()),
                new ToolInfo("get_error_logs", "Retrieve ERROR/FATAL/CRITICAL log entries from a source", Map.of()),
                new ToolInfo("analyze_logs", "Analyze log evidence and provide likely causes, confidence and remediation", Map.of()),
                new ToolInfo("find_root_cause", "Investigate likely root causes from bounded log evidence", Map.of()),
                new ToolInfo("correlate_logs", "Group events by shared trace ID and request ID", Map.of()),
                new ToolInfo("detect_anomalies", "Identify unusual severity concentration and repeated error signatures", Map.of()),
                new ToolInfo("get_service_health", "Summarize severity distribution and latest observed errors", Map.of()),
                new ToolInfo("suggest_fixes", "Suggest safe remediation ideas grounded in log evidence", Map.of()),
                new ToolInfo("find_trace_logs", "Find events with a distributed trace identifier", Map.of())
        );
    }

    public List<PromptSuggestion> getPromptSuggestions() {
        return List.of(
                new PromptSuggestion("payment_error", "Payment Pool Timeout", "Show me all ERROR and FATAL logs in sample-logs/payment-service.log", "Payment", "diagnostic"),
                new PromptSuggestion("auth_ratelimit", "Auth Rate Limiting", "Investigate failed authentication and brute force attempts in sample-logs/auth-service.log", "Auth", "security"),
                new PromptSuggestion("order_failure", "Order Checkout Failures", "Investigate root cause of downstream timeout in sample-logs/order-service.log", "Orders", "investigation"),
                new PromptSuggestion("gateway_504", "Gateway 504 Timeouts", "Analyze 504 Gateway Timeout errors in sample-logs/api-gateway.log", "Gateway", "networking"),
                new PromptSuggestion("db_deadlock", "Database Deadlocks", "Detect deadlock and statement_timeout errors in sample-logs/database-cluster.log", "Database", "database"),
                new PromptSuggestion("k8s_oom", "K8s OOMKilled Pods", "Find OOMKilled events and container crash loops in sample-logs/k8s-pod-events.log", "Kubernetes", "infrastructure"),
                new PromptSuggestion("notif_quota", "Notification Quota Errors", "Analyze 429 quota and SMTP delivery failures in sample-logs/notification-service.json.log", "Notifications", "integration"),
                new PromptSuggestion("trace_correlate", "Trace tr-1001 Across Services", "Correlate trace ID tr-1001 across services in sample-logs/payment-service.log", "Traces", "investigation"),
                new PromptSuggestion("sources", "List MCP Log Sources", "Which log connector types and sources are available in LogSense?", "Sources", "discovery")
        );
    }

    public ChatResponse processChat(String userMessage) {
        String trimmed = userMessage == null ? "" : userMessage.trim();
        if (trimmed.isEmpty()) {
            return new ChatResponse("Please ask a question or click one of the suggested prompts below.", null, null, getPromptSuggestions());
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        String defaultType = "FILE";
        String defaultSource = detectSourceFile(trimmed);

        String matchedTool = null;
        Map<String, Object> toolArgs = new HashMap<>();

        if (lower.contains("source") || lower.contains("connectors")) {
            matchedTool = "list_log_sources";
        } else if (lower.contains("root cause") || lower.contains("why did it fail") || lower.contains("rootcause")) {
            matchedTool = "find_root_cause";
            toolArgs.put("type", defaultType);
            toolArgs.put("source", defaultSource);
            toolArgs.put("incident", trimmed);
        } else if (lower.contains("trace") || lower.contains("correlat")) {
            String traceId = extractTraceId(trimmed);
            if (traceId != null) {
                matchedTool = "correlate_logs";
                toolArgs.put("type", defaultType);
                toolArgs.put("source", defaultSource);
                toolArgs.put("correlationId", traceId);
            } else {
                matchedTool = "analyze_logs";
                toolArgs.put("type", defaultType);
                toolArgs.put("source", defaultSource);
                toolArgs.put("question", trimmed);
            }
        } else if (lower.contains("anomal") || lower.contains("signature")) {
            matchedTool = "detect_anomalies";
            toolArgs.put("type", defaultType);
            toolArgs.put("source", defaultSource);
        } else if (lower.contains("health") || lower.contains("status") || lower.contains("distribution")) {
            matchedTool = "get_service_health";
            toolArgs.put("type", defaultType);
            toolArgs.put("source", defaultSource);
        } else if (lower.contains("error") || lower.contains("fatal") || lower.contains("failed") || lower.contains("timeout")) {
            matchedTool = "get_error_logs";
            toolArgs.put("type", defaultType);
            toolArgs.put("source", defaultSource);
            toolArgs.put("limit", 50);
        } else {
            matchedTool = "analyze_logs";
            toolArgs.put("type", defaultType);
            toolArgs.put("source", defaultSource);
            toolArgs.put("question", trimmed);
        }

        Object toolResult = executeTool(matchedTool, toolArgs);

        String answer;
        try {
            String systemPrompt = """
                    You are LogSense AI Assistant, an SRE and DevOps log analysis expert.
                    An MCP tool was executed to inspect the system logs.
                    Review the user's inquiry and the raw tool output.
                    Provide a concise, clear, and actionable explanation with bullet points and concrete evidence.
                    Distinguish facts from hypotheses. Highlight root causes and safe remediation steps if applicable.
                    """;

            String prompt = "User Question: " + trimmed + "\n\nMCP Tool Used: " + matchedTool +
                    "\n\nTool Output: " + objectMapper.writeValueAsString(toolResult);

            answer = chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.info("ChatClient invocation completed or bypassed: {}", ex.getMessage());
            answer = generateDeterministicSummary(matchedTool, toolResult, trimmed);
        }

        List<PromptSuggestion> next = getNextSuggestions(matchedTool);
        return new ChatResponse(answer, matchedTool, toolResult, next);
    }

    private String detectSourceFile(String input) {
        if (input == null) return "sample-logs/payment-service.log";
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.contains("auth")) return "sample-logs/auth-service.log";
        if (lower.contains("order")) return "sample-logs/order-service.log";
        if (lower.contains("gateway")) return "sample-logs/api-gateway.log";
        if (lower.contains("database") || lower.contains("deadlock")) return "sample-logs/database-cluster.log";
        if (lower.contains("k8s") || lower.contains("pod") || lower.contains("oom")) return "sample-logs/k8s-pod-events.log";
        if (lower.contains("notif") || lower.contains("twilio") || lower.contains("sms") || lower.contains("quota")) return "sample-logs/notification-service.json.log";
        return "sample-logs/payment-service.log";
    }

    private String extractTraceId(String input) {
        var matcher = java.util.regex.Pattern.compile("\\b(tr-[a-zA-Z0-9_-]+|req-[a-zA-Z0-9_-]+)\\b").matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "tr-1001";
    }

    private Object executeTool(String toolName, Map<String, Object> args) {
        if (mcpSyncClient != null) {
            try {
                var callRequest = McpSchema.CallToolRequest.builder()
                        .name(toolName)
                        .arguments(args)
                        .build();
                var result = mcpSyncClient.callTool(callRequest);
                if (result != null && result.content() != null && !result.content().isEmpty()) {
                    var first = result.content().get(0);
                    if (first instanceof McpSchema.TextContent textContent) {
                        try {
                            return objectMapper.readValue(textContent.text(), Object.class);
                        } catch (Exception parseEx) {
                            return textContent.text();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("MCP client tool call fallback to service execution: {}", e.getMessage());
            }
        }

        return executeDirectFallback(toolName, args);
    }

    private Object executeDirectFallback(String toolName, Map<String, Object> args) {
        String type = (String) args.getOrDefault("type", "FILE");
        String source = (String) args.getOrDefault("source", "sample-logs/payment-service.log");
        Integer limit = (Integer) args.getOrDefault("limit", 100);

        return switch (toolName) {
            case "list_log_sources" -> Map.of("connectors", searchService.supportedTypes(), "access", "read-only");
            case "get_error_logs" -> {
                List<LogEvent> rows = searchService.search(type, source, null, null, null, null, limit);
                yield rows.stream().filter(e -> Set.of("ERROR", "FATAL", "CRITICAL").contains(e.severity())).toList();
            }
            case "find_root_cause" -> {
                String incident = (String) args.getOrDefault("incident", "System error");
                yield analysisService.analyze(searchService.search(type, source, null, null, null, null, limit), "Find likely root causes for: " + incident);
            }
            case "correlate_logs" -> {
                String corr = (String) args.getOrDefault("correlationId", "tr-1001");
                List<LogEvent> rows = searchService.search(type, source, null, null, null, corr, limit);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("traceId", corr);
                m.put("events", rows);
                m.put("eventCount", rows.size());
                yield m;
            }
            case "detect_anomalies" -> {
                List<LogEvent> rows = searchService.search(type, source, null, null, null, null, limit);
                long errors = rows.stream().filter(e -> Set.of("ERROR", "FATAL", "CRITICAL").contains(e.severity())).count();
                yield Map.of("sampleSize", rows.size(), "errorCount", errors, "errorRatio", rows.isEmpty() ? 0.0 : (double) errors / rows.size());
            }
            case "get_service_health" -> {
                List<LogEvent> rows = searchService.search(type, source, null, null, null, null, limit);
                Map<String, Long> counts = rows.stream().collect(java.util.stream.Collectors.groupingBy(LogEvent::severity, java.util.stream.Collectors.counting()));
                List<LogEvent> latestErrors = rows.stream().filter(e -> Set.of("ERROR", "FATAL", "CRITICAL").contains(e.severity())).toList();
                yield Map.of("sampleSize", rows.size(), "severityCounts", counts, "latestErrors", latestErrors, "healthAssessment", latestErrors.isEmpty() ? "Healthy" : "Degraded");
            }
            default -> {
                String q = (String) args.getOrDefault("question", "Summarize logs");
                yield analysisService.analyze(searchService.search(type, source, null, null, null, null, limit), q);
            }
        };
    }

    private String generateDeterministicSummary(String tool, Object result, String userQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("### MCP Analysis Result (Tool: `").append(tool).append("`)\n\n");
        if ("list_log_sources".equals(tool)) {
            sb.append("Supported connector sources: `FILE`, `DOCKER`, `KUBERNETES`, `ELASTICSEARCH`, `OPENSEARCH`, `CLOUDWATCH`, `LOKI`, `SPLUNK`, `POSTGRESQL`, `KAFKA`.\n");
        } else if ("get_error_logs".equals(tool) && result instanceof List<?> list) {
            sb.append("Found **").append(list.size()).append(" error event(s)** in the targeted log source.\n\n");
            for (Object obj : list) {
                if (obj instanceof LogEvent evt) {
                    sb.append("- `[").append(evt.severity()).append("]` **").append(evt.service()).append("**: ")
                            .append(evt.message()).append("\n");
                }
            }
        } else if ("correlate_logs".equals(tool) && result instanceof Map<?,?> map) {
            Object count = map.containsKey("eventCount") ? map.get("eventCount") : 0;
            sb.append("Trace correlation complete. Grouped **").append(count)
                    .append(" event(s)** matching correlation identifier.\n");
        } else if ("get_service_health".equals(tool) && result instanceof Map<?,?> map) {
            sb.append("Health Assessment: **").append(map.get("healthAssessment")).append("**\n");
            sb.append("- Severity Distribution: ").append(map.get("severityCounts")).append("\n");
        } else {
            sb.append("Analyzed log events for question: *\"").append(userQuery).append("\"*.\n");
            sb.append("Structured diagnostic evidence retrieved from log store.");
        }
        return sb.toString();
    }

    private List<PromptSuggestion> getNextSuggestions(String lastTool) {
        if ("get_error_logs".equals(lastTool)) {
            return List.of(
                    new PromptSuggestion("rca", "Investigate Root Cause", "Investigate root cause of connection timeout in sample-logs/payment-service.log", "RCA", "investigation"),
                    new PromptSuggestion("correlate", "Trace tr-1001", "Correlate trace ID tr-1001 across services in sample-logs/payment-service.log", "Traces", "investigation")
            );
        } else if ("find_root_cause".equals(lastTool)) {
            return List.of(
                    new PromptSuggestion("fixes", "Suggest Remediations", "Suggest safe fixes for database pool exhaustion", "Fixes", "remediation"),
                    new PromptSuggestion("health", "Check Service Health", "What is the health status and severity distribution for sample-logs/payment-service.log?", "Health", "monitoring")
            );
        }
        return getPromptSuggestions();
    }
}
