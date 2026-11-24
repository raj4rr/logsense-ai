# Connector implementation matrix

All connector implementations must implement `LogConnector` and return normalized `LogEvent` records. Each must have a source registry record with type, enabled flag, tenant, credential reference (never raw secret in JSON), and an allowlisted endpoint/resource identifier.

| Connector | Implementation approach | Required configuration / safety |
|---|---|---|
| Local files | Implemented: UTF-8 line reader | Canonical path must remain under `logsense.files.allowed-roots`; read-only mount; bounded results |
| Docker | Docker Engine API or docker-java | Socket/API read-only; container ID allowlist; tail/time caps; never exec |
| Kubernetes | Kubernetes Java client / API logs endpoint | ServiceAccount limited to `get/list` pods and `get` pods/log in named namespaces; no exec/delete |
| Elasticsearch / OpenSearch | Java API client or signed REST query | Read-only role; index allowlist; query DSL generated server-side; timeout and max hits |
| AWS CloudWatch | AWS SDK v2 `CloudWatchLogsClient` | IAM role with `logs:DescribeLogGroups`, `logs:DescribeLogStreams`, `logs:FilterLogEvents`; restrict log groups |
| Grafana Loki | HTTP `/loki/api/v1/query_range` | Read-only token; fixed tenant; cap `limit`, time range and query length |
| Splunk | Search Jobs REST API | Read-only role; allowlisted indexes; enforce earliest/latest time and job TTL |
| PostgreSQL | Separate read-only DataSource | Dedicated DB role with SELECT on approved log tables/views; statement timeout; no arbitrary SQL input |
| Kafka | KafkaConsumer with dedicated group and bounded poll | Read-only ACL; allowlisted topics; do not commit offsets unless explicitly designed; cap records/time |

## Connector completion checklist

1. Add connector-specific configuration properties and secret references.
2. Validate endpoint host against an administrator-managed allowlist to prevent SSRF.
3. Add bounded timeouts, retries with jitter, pagination and cancellation.
4. Normalize timestamps to UTC and severity to TRACE/DEBUG/INFO/WARN/ERROR/FATAL.
5. Preserve trace_id, span_id, request_id, service, environment, host and source metadata.
6. Redact secrets/PII before persistence, caching, or LLM submission.
7. Add unit tests with mocked clients and integration tests against Testcontainers or local service containers.
8. Emit audit events for every source query, including actor, tenant, source, filters, result count and outcome.
