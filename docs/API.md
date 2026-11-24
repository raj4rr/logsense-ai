# REST API

All `/api/v1/**` routes require `X-API-Key: <key>` unless JWT resource-server mode is configured.

* `GET /api/v1/sources` – connector types registered in the runtime.
* `GET /api/v1/logs?type=FILE&source=/var/log/logsense/payment-service.log&severity=ERROR&limit=100` – bounded read.
* `POST /api/v1/analyze` – JSON body: `{"type":"FILE","source":"/var/log/logsense/payment-service.log","question":"Why are payments failing?","limit":100}`.

Optional query fields: `keyword`, `severity`, `service`, `traceId`, `from`, `to`, `limit`. Date/time values are ISO-8601 instants. Limit is 1–500.

MCP tools: `list_log_sources`, `search_logs`, `get_error_logs`, `analyze_logs`, `find_root_cause`, `correlate_logs`, `find_trace_logs`, `detect_anomalies`, `analyze_performance`, `get_service_health`, `suggest_fixes`, and `generate_incident_report`. MCP server uses Streamable HTTP; verify your MCP client transport configuration and base path against the Spring AI MCP server version selected in `pom.xml`.
