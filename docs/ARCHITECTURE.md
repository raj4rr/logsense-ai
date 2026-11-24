# LogSense AI architecture

## Request path

`MCP client / REST client -> Spring Security API-key gate -> MCP/REST adapter -> LogSearchService -> ConnectorRegistry -> LogConnector -> normalized LogEvent -> deterministic analysis -> Spring AI ChatClient (Ollama or OpenAI)`.

The connector contract keeps the MCP layer independent from vendor-specific APIs. Connector adapters must be read-only and enforce source allowlists, query timeouts, result caps, and tenant authorization before any request reaches a source.

## MVP boundaries

This repository is a runnable starter for local-file retrieval, MCP tool registration, REST endpoints, deterministic summaries, AI analysis, Postgres migrations, Redis service wiring, and Compose deployment. Nine remote-source connector names are reserved and represented by a fail-closed adapter. They are not falsely represented as production-ready integrations; see CONNECTORS.md for the implementation contracts and required credentials.

## SecureMCP reuse

The prior SecureMCP design included RS256/JWKS, OAuth/OIDC/PKCE, JDBC registered clients, tenant-aware PostgreSQL identity/RBAC/ABAC, per-tool authorization, Redis-backed revocation/rate limits, API keys, and audit events. This MVP deliberately keeps a small API-key development gate and does not copy a second identity provider. For production, replace `ApiKeyFilter` with the existing SecureMCP JWT/resource-server filter and invoke its authorization service for every source and tool. Map `LOG_READER` to search/analyze and reserve source administration for `LOG_ADMIN`. Persist MCP calls to the existing audit service/table. Do not deploy the starter API key as a production identity solution.
