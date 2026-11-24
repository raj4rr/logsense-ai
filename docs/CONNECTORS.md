# Connector configuration and operational notes

All connectors implement `LogConnector` and return the same normalized `LogEvent` structure. All access is intended to be read-only. Configure endpoints and credentials through environment variables/secrets; never commit credentials.

| Type | Source argument | Required configuration | Notes |
|---|---|---|---|
| FILE | absolute file path | `LOGSENSE_ALLOWED_ROOTS` | Path must remain under an allowed root. Mount logs read-only. |
| DOCKER | container ID/name | `DOCKER_ENABLED=true`, `DOCKER_HOST` | Docker socket access is effectively host-root access; use only on trusted hosts. |
| KUBERNETES | namespace | `KUBERNETES_ENABLED=true`, `KUBERNETES_API_URL`, `KUBERNETES_TOKEN`, namespace | Token needs only `get/list pods` and `get pods/log` in selected namespace. |
| ELASTICSEARCH / OPENSEARCH | index/alias | `ELASTICSEARCH_URL`, optional bearer token | Uses `_search`; grant index read-only privilege. |
| CLOUDWATCH | log group name | `AWS_REGION` and AWS default credential chain | Grant only `logs:FilterLogEvents` on selected log groups. |
| LOKI | LogQL selector | `LOKI_URL`, optional token | Example selector `{app="payments"}`. |
| SPLUNK | index name | `SPLUNK_URL`, `SPLUNK_TOKEN`, `SPLUNK_INDEX` | Token should have search capability only. The adapter constructs a restricted index search and expects JSON export response; verify against your Splunk version. |
| POSTGRESQL | configured table name | `SOURCE_POSTGRES_URL/USER/PASSWORD/TABLE` | Read-only account and table allowlist; schema expected: occurred_at, severity, message, service_name, trace_id, request_id, host. |
| KAFKA | topic | `KAFKA_BOOTSTRAP_SERVERS` | Consumer is read-only and does not commit offsets. The MVP samples bounded recent topic data; use a dedicated ACL-limited principal. |

Remote connectors require live infrastructure, valid endpoint versions and credentials for integration verification. CloudWatch uses the AWS SDK default credential chain. For Kubernetes, configure TLS trust properly; do not disable certificate validation in production.
