# LogSense AI – Java 21 MCP Log Intelligence

Read-only, multi-source log search and AI-assisted troubleshooting using Java 21, Spring Boot, Spring AI MCP, Ollama/OpenAI, PostgreSQL and Redis.

## Features
- Normalized `LogEvent` model and connector registry.
- Read-only adapters for local files, Docker, Kubernetes, Elasticsearch/OpenSearch, AWS CloudWatch Logs, Grafana Loki, Splunk, PostgreSQL and Kafka.
- MCP tools: `list_log_sources`, `search_logs`, `get_error_logs`, `analyze_logs`, `find_root_cause`, `correlate_logs`, `find_trace_logs`, `detect_anomalies`, `analyze_performance`, `get_service_health`, `suggest_fixes`, `generate_incident_report`.
- REST APIs for source listing, bounded log search and analysis.
- AI provider selection using `AI_PROVIDER=ollama|openai`.
- Evidence-first prompt, recurring error fingerprints, severity counts and safe diagnostic recommendations.
- API key bootstrap and optional JWT issuer validation.

## Quick start
1. Install Docker Desktop / Docker Engine with Compose and Java 21 (Maven is needed for local builds).
2. `cp .env.example .env` and replace all development secrets with random values. Load the variables into your shell before using the curl examples: `set -a; . ./.env; set +a`.
3. Start the local AI profile: `docker compose --profile local-ai up --build -d`.
4. Pull a local model: `docker compose exec ollama ollama pull qwen3:8b`.
5. Health: `curl http://localhost:8080/actuator/health`.
6. Search sample logs:

```bash
curl -H "X-API-Key: $LOGSENSE_API_KEY"  'http://localhost:8080/api/v1/logs?type=FILE&source=/var/log/logsense/payment-service.log&severity=ERROR&limit=50'
```

7. Analyze:

```bash
curl -X POST http://localhost:8080/api/v1/analyze  -H 'Content-Type: application/json' -H "X-API-Key: $LOGSENSE_API_KEY"  -d '{"type":"FILE","source":"/var/log/logsense/payment-service.log","question":"Explain payment failures and likely causes","limit":100}'
```

For OpenAI set `AI_PROVIDER=openai` and `OPENAI_API_KEY` in `.env`; restart app. Do not configure an API key in source control.

## Build and test locally
`mvn clean verify` (requires Maven 3.9+ and Java 21). The Dockerfile performs a Maven package build. This source bundle has not been run against your live cloud accounts, clusters, Splunk, Kafka or external databases; validate each connector in a non-production environment before rollout.

## Configure connectors
See [docs/CONNECTORS.md](docs/CONNECTORS.md). For security, use read-only credentials and fixed endpoint configuration. Do not pass remote URLs or credentials as MCP tool arguments.

## Security and SecureMCP
See [docs/SECURITY.md](docs/SECURITY.md). Existing SecureMCP can provide the production identity, tenant authorization and audit layer. Configure `JWT_ISSUER_URI` for JWT validation, then integrate SecureMCP's authorization/audit services at the adapter boundary. The API-key filter is bootstrap security, not a substitute for tenant-aware authorization.

## Important MVP boundary
This is an on-demand read/search and analysis service. It is not yet a persistent tailing/streaming ingestion system, full SIEM, automatic remediation agent, or verified production integration. Redis is provisioned but query caching/rate limiting are extension points. Redis provides a short five-second query-result cache. The SQL migration includes source/event/audit tables; current source reads are direct and do not automatically persist every retrieved event.
