# Security notes

- The sample API key filter is for local MVP use only. Use a high-entropy secret and rotate it. Do not expose the service publicly with this filter.
- Production integration: use SecureMCP's RS256/JWKS token validation, OAuth2/OIDC clients, tenant-aware RBAC/ABAC, per-tool policies, Redis rate limiting/revocation, and audit pipeline.
- Give connectors read-only permissions. No MCP tool in this MVP performs remediation or shell execution.
- Mount local log directories read-only and restrict them with `LOGSENSE_ALLOWED_ROOTS`.
- Never mount the Docker socket in a production environment without a dedicated read-only proxy; Docker socket access can be effectively root-equivalent despite a read-only bind mount.
- Kubernetes RBAC should only allow reading pods and pod logs in approved namespaces.
- Treat log contents as untrusted prompt input. The analysis prompt explicitly says not to follow embedded instructions; additionally redact secrets before model calls and do not send regulated data to hosted models without approval.
- Configure TLS at an ingress/reverse proxy, rotate credentials, and use external secret management.
