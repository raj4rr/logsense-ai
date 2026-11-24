package com.logsense.config;
import org.springframework.boot.context.properties.ConfigurationProperties; import java.util.List;
@ConfigurationProperties(prefix="logsense")
public record LogSenseProperties(String aiProvider, Security security, Files files, Limits limits, Sources sources) {
 public record Security(String apiKey, boolean allowLocalNoKey, String issuerUri) {}
 public record Files(List<String> allowedRoots) {}
 public record Limits(Integer maxResults, Integer maxWindowHours) {}
 public record Sources(Docker docker, Kubernetes kubernetes, HttpSource elasticsearch, HttpSource loki, Splunk splunk, CloudWatch cloudwatch, Kafka kafka, Postgres postgres) {}
 public record Docker(String host, boolean enabled) {}
 public record Kubernetes(String apiUrl, String bearerToken, String namespace, boolean enabled) {}
 public record HttpSource(String url, String username, String password, String token) {}
 public record Splunk(String url, String token, String index) {}
 public record CloudWatch(String region) {}
 public record Kafka(String bootstrapServers, String groupId, String securityProtocol, String saslMechanism, String saslJaasConfig) {}
 public record Postgres(String url, String username, String password, String table) {}
}
