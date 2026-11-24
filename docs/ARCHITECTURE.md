# Architecture

`ConnectorRegistry -> LogConnector adapters -> normalized LogEvent -> LogSearchService -> LogAnalysisService -> ChatClient`.

The MCP Streamable HTTP server exposes `list_log_sources`, `search_logs`, `analyze_logs`, and `find_trace_logs`. REST API is under `/api/v1`. PostgreSQL stores source metadata, optional normalized event persistence and audit records; Redis provides a five-second best-effort query-result cache; rate limiting remains a deployment extension. The MVP performs bounded on-demand reads from configured sources rather than shipping an always-on tailing/streaming ingestion daemon. Search results are capped at 500 and the time window is capped at 168 hours by default.

The normalized event includes source, service, environment, severity, timestamp, message, logger, traceId, spanId, requestId, host and attributes. Correlation is currently identifier-based (trace/request ID); causal root-cause ranking and persistent topology graphs are future hardening work.

## Deployment boundary

Docker Compose starts the application, PostgreSQL, Redis and optional Ollama. The app receives source endpoints/secrets as environment variables. Do not expose PostgreSQL, Redis, Ollama or MCP endpoints publicly without network controls and authentication.
