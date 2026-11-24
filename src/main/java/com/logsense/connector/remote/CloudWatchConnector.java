package com.logsense.connector.remote;

import com.logsense.config.LogSenseProperties;
import com.logsense.connector.*;
import com.logsense.connector.file.LocalFileConnector;
import com.logsense.model.*;
import com.logsense.model.LogEvent;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.time.*;
import java.util.*;

@Component
public class CloudWatchConnector implements LogConnector {
    private final LogSenseProperties p;

    public CloudWatchConnector(LogSenseProperties p) {
        this.p = p;
    }

    public String type() {
        return "CLOUDWATCH";
    }

    public boolean supports(String t) {
        return "CLOUDWATCH".equalsIgnoreCase(t);
    }

    public List<LogEvent> search(String group, LogQuery q) {
        var c = p.sources().cloudwatch();
        if (c == null || c.region() == null || c.region().isBlank())
            throw new IllegalStateException("Set AWS_REGION and configure AWS credentials via the default provider chain");
        Instant from = q.from() == null ? Instant.now().minus(Duration.ofHours(1)) : q.from();
        try (CloudWatchLogsClient client = CloudWatchLogsClient.builder().region(Region.of(c.region())).build()) {
            FilterLogEventsRequest req = FilterLogEventsRequest.builder().logGroupName(group).startTime(from.toEpochMilli()).endTime((q.to() == null ? Instant.now() : q.to()).toEpochMilli()).limit(q.safeLimit()).filterPattern(q.keyword() == null ? null : q.keyword()).build();
            List<LogEvent> out = new ArrayList<>();
            for (var e : client.filterLogEvents(req).events()) {
                LogEvent mapped = LogEventMapper.fromText(group, e.message(), Instant.ofEpochMilli(e.timestamp()), Map.of("logStream", e.logStreamName() == null ? "" : e.logStreamName()));
                if (LocalFileConnector.matches(mapped, q)) out.add(mapped);
            }
            return out;
        }
    }
}
