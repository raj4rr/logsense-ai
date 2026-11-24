# Security model

* Use API keys for local/dev bootstrap; configure `JWT_ISSUER_URI` to enable Spring Security OAuth2 JWT resource-server validation in an identity-provider deployment.
* Reuse SecureMCP by extracting its authentication/authorization/audit services behind an adapter interface. Do not copy secrets or duplicate token issuance into LogSense. Map SecureMCP roles to `LOG_READER`, `LOG_ADMIN`, and tenant/source scopes.
* All connectors must use read-only credentials. Kubernetes: get/list pods and get pods/log only. AWS: logs:FilterLogEvents only. Elasticsearch: read privileges only. Splunk: search only. PostgreSQL: SELECT on an approved view/table. Kafka: READ/DESCRIBE only.
* Docker socket mounting is highly privileged even if the mount is marked read-only; prefer a restricted remote Docker proxy or omit Docker connector in shared production clusters.
* Restrict file connector roots and mount source log folders read-only. Do not accept arbitrary URLs from MCP callers: endpoints are deployment configuration, not tool parameters.
* Redact credentials, authorization headers, session cookies, email/phone/payment identifiers before sending evidence to an external model. Add tenant-aware redaction before production use.
* Treat all log content as untrusted prompt data. The analysis prompt explicitly prohibits following embedded instructions. This is not a substitute for model-side isolation or output validation.
* Cap query size, duration and response payload. Apply network egress allowlists and secret manager injection.
* Never execute remediation automatically. Recommendations are advisory and require operator approval.
* Persist audit events for actor, tenant, connector, source, query window, outcome and event count. The migration creates the audit table; wire your SecureMCP audit writer or an application audit service before production.

## SecureMCP adapter hooks

`SecureMcpAuthorizationPort` is the extension point for your existing SecureMCP RBAC/ABAC service. Supply a Spring bean implementing it to replace `DefaultRoleAuthorizationAdapter`; enforce tenant membership and source-level policy using the `tenant_id` claim and the requested connector/source. `AuditSink` is the extension point for your SecureMCP audit service; supply a bean to replace `JdbcAuditSink`. This keeps LogSense independent of SecureMCP package names while allowing direct reuse of its services. The default role adapter is a development baseline, not tenant-aware production authorization.

`LogRedactor` removes common password/token/API-key patterns, bearer/basic authorization values, JWT-shaped strings, AWS access-key IDs and cookie values from the text sent to the LLM. Add organization-specific patterns (customer IDs, payment data, internal secrets) before production rollout.
