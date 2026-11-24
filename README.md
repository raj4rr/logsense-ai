# LogSense AI – MCP Server MVP

Java 21 · Spring Boot 4.2.0 · Spring AI 2.1.0 · MCP Streamable HTTP · PostgreSQL · Redis · Ollama/OpenAI · Docker Compose

## What is included

- Runnable Spring Boot application and Maven build.
- Spring AI MCP Streamable HTTP server with `list_log_sources`, `search_logs`, and `analyze_logs` tools.
- REST API for source listing, log search and AI analysis.
- Local file connector with configured-root path traversal protection.
- Common `LogEvent` model and `LogConnector` SPI.
- Ollama/OpenAI model starters; select provider using `AI_PROVIDER` and model environment settings.
- PostgreSQL schema migration for sources, normalized events and audit events; Redis service in Compose.
- API-key development filter and SecureMCP integration guidance.
- Docker Compose for app, PostgreSQL, Redis and optional Ollama.

**Connector status:** Local files are implemented. Docker, Kubernetes, Elasticsearch/OpenSearch, CloudWatch, Loki, Splunk, PostgreSQL log-table and Kafka connector types are declared but fail closed until their vendor clients, credentials and allowlists are configured. This is an honest, extensible MVP scaffold—not a claim that nine vendor integrations are already production complete.

## Requirements

- Java 21
- Maven 3.9+
- Docker Desktop / Docker Engine with Compose

## Run with Docker Compose

```bash
cp .env.example .env
# Edit .env: set strong DB_PASSWORD and LOGSENSE_API_KEY
docker compose up --build -d
```

For local Ollama, enable the profile and pull a model:

```bash
docker compose --profile local-ai up --build -d
# In another terminal:
docker compose exec ollama ollama pull qwen3:8b
```

Set `AI_PROVIDER=ollama` in `.env`. For OpenAI, set `AI_PROVIDER=openai` and `OPENAI_API_KEY=...`; the model defaults to `gpt-4.1-mini`.

Check health:

```bash
curl http://localhost:8080/actuator/health
```

Search sample logs (the sample file is mounted at `/var/log/logsense/payment-service.log`):

```bash
curl -H "X-API-Key: $(grep '^LOGSENSE_API_KEY=' .env | cut -d= -f2-)" \
 'http://localhost:8080/api/v1/logs?type=FILE&source=%2Fvar%2Flog%2Flogsense%2Fpayment-service.log&severity=ERROR'
```

Analyze logs:

```bash
curl -X POST http://localhost:8080/api/v1/analyze \
 -H 'Content-Type: application/json' -H "X-API-Key: YOUR_KEY" \
 -d '{"type":"FILE","source":"/var/log/logsense/payment-service.log","limit":100,"question":"Explain the errors and likely root cause."}'
```

## Run locally without Compose

Create a PostgreSQL database named `logsense`, run Redis, set environment variables, then:

```bash
mvn spring-boot:run
```

For a local file, set `LOGSENSE_ALLOWED_ROOTS=./sample-logs` and use the absolute path to `sample-logs/payment-service.log` as `source`.

## Tests

```bash
mvn test
```

## MCP client configuration

Connect a Streamable HTTP MCP client to `http://localhost:8080/mcp`. Supply `X-API-Key` in the transport's HTTP headers. The MCP tools are documented in `docs/API.md`.

## Configuration

See `src/main/resources/application.yml` and `.env.example`. Never commit `.env` or real credentials.

## Architecture and next steps

- `docs/ARCHITECTURE.md`
- `docs/CONNECTORS.md`
- `docs/SECURITY.md`
- `docs/API.md`

## Version note

The project targets Spring Boot 4.2.0 and Spring AI 2.1.0, released November 2026. It uses the Spring AI MCP WebMVC starter and the interoperable Streamable HTTP protocol mode. Check client protocol support before switching to the newer stateless MCP revision.
