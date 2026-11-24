package com.logsense.api;

import com.logsense.analysis.LogAnalysisService;
import com.logsense.model.LogEvent;
import com.logsense.service.LogSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.*;

@RestController
@Validated
@RequestMapping("/api/v1")
public class LogController {
    private final LogSearchService search;
    private final LogAnalysisService analysis;

    public LogController(LogSearchService s, LogAnalysisService a) {
        search = s;
        analysis = a;
    }

    @GetMapping("/sources")
    public Map<String, Object> sources() {
        return Map.of("connectors", search.supportedTypes(), "readOnly", true);
    }

    @GetMapping("/logs")
    public List<LogEvent> logs(@RequestParam String type, @RequestParam String source, @RequestParam(required = false) String keyword, @RequestParam(required = false) String severity, @RequestParam(required = false) String service, @RequestParam(required = false) String traceId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to, @RequestParam(defaultValue = "100") @Min(1) @Max(500) Integer limit) {
        return search.search(type, source, keyword, severity, service, traceId, from, to, limit);
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@Valid @RequestBody AnalyzeRequest r) {
        var events = search.search(r.type(), r.source(), r.keyword(), r.severity(), r.service(), r.traceId(), r.from(), r.to(), r.limit());
        return analysis.analyze(events, r.question() == null ? "Explain the errors and likely root causes." : r.question());
    }

    public record AnalyzeRequest(@NotBlank String type, @NotBlank String source, String keyword, String severity,
                                 String service, String traceId, Instant from, Instant to,
                                 @Min(1) @Max(500) Integer limit, String question) {
    }
}
