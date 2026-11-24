# REST API

All endpoints except `/actuator/health/**` require `X-API-Key: <key>` (or `Authorization: Bearer <key>` in this development build).

## GET /api/v1/sources
Returns connector type names and the currently active starter connectors.

## GET /api/v1/logs
Parameters: `type` (required), `source` (required), `keyword`, `severity`, `service`, `traceId`, `limit` (1-500).

Example: `curl -H 'X-API-Key: change-me-local' 'http://localhost:8080/api/v1/logs?type=FILE&source=%2Fvar%2Flog%2Flogsense%2Fpayment-service.log&severity=ERROR'`

## POST /api/v1/analyze
JSON body: `{"type":"FILE","source":"/var/log/logsense/payment-service.log","severity":"ERROR","limit":100,"question":"What is the likely root cause?"}`.

## MCP
Spring AI Streamable HTTP MCP endpoint is served by the MCP starter (default endpoint path `/mcp`). Tools: `list_log_sources`, `search_logs`, `analyze_logs`.
