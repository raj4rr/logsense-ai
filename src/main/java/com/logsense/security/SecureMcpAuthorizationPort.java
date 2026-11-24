package com.logsense.security;

import org.springframework.security.core.Authentication;

/** Adapter seam for reusing SecureMCP's tenant RBAC/ABAC authorization service. */
public interface SecureMcpAuthorizationPort {
    boolean canRead(Authentication authentication, String tenantId, String connectorType, String source);
}
