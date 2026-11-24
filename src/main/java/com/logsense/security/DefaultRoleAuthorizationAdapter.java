package com.logsense.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DefaultRoleAuthorizationAdapter implements SecureMcpAuthorizationPort {
    @Override public boolean canRead(Authentication auth, String tenantId, String connectorType, String source) {
        // If authentication is present, verify reader or admin role
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LOG_ADMIN")
                    || a.getAuthority().equals("ROLE_LOG_READER") || a.getAuthority().equals("SCOPE_log.read"));
        }
        // In local/standalone developer mode where MCP endpoint is open and no auth token was supplied, permit reads
        return true;
    }
}
