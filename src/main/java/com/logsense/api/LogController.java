package com.logsense.api;

import com.logsense.analysis.LogAnalysisService;
import com.logsense.model.LogEvent;
import com.logsense.service.LogSearchService;
import jakarta.validation.constraints.Max;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class LogController {
    private final LogSearchService search;
    private final LogAnalysisService analysis;
    public LogController(LogSearchService search, LogAnalysisService analysis) { this.search=search; this.analysis=analysis; }
    @GetMapping("/sources") public Map<String,Object> sources() { return Map.of("supportedTypes", List.of("FILE","DOCKER","KUBERNETES","ELASTICSEARCH","OPENSEARCH","CLOUDWATCH","LOKI","SPLUNK","POSTGRESQL","KAFKA"), "note", "Only FILE is executable in the starter profile; remote connector wiring is documented."); }
    @GetMapping("/logs") public List<LogEvent> logs(@RequestParam String type, @RequestParam String source,
      @RequestParam(required=false) String keyword, @RequestParam(required=false) String severity,
      @RequestParam(required=false) String service, @RequestParam(required=false) String traceId,
      @RequestParam(defaultValue="100") @Max(500) Integer limit) {
        return search.search(type, source, keyword, severity, service, traceId, limit);
    }
    @PostMapping("/analyze") public Map<String,Object> analyze(@RequestBody AnalyzeRequest req) {
        List<LogEvent> events=search.search(req.type(),req.source(),req.keyword(),req.severity(),null,req.traceId(),req.limit());
        return analysis.analyze(events, req.question()==null?"Explain the errors and identify likely root causes.":req.question());
    }
    public record AnalyzeRequest(String type,String source,String keyword,String severity,String traceId,Integer limit,String question) {}
}
