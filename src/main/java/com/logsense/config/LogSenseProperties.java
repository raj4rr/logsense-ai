package com.logsense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "logsense")
public record LogSenseProperties(String aiProvider, Security security, Files files, Limits limits) {
    public record Security(String apiKey, boolean allowLocalNoKey) {}
    public record Files(List<String> allowedRoots) {}
    public record Limits(Integer maxResults, Integer maxWindowHours) {}
}
