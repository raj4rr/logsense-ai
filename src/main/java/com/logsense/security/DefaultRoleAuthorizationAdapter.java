package com.logsense.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DefaultRoleAuthorizationAdapter implements SecureMcpAuthorizationPort {
    @Override public boolean canRead(Authentication auth, String tenantId, String connectorType, String source) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_LOG_ADMIN")
                || a.getAuthority().equals("ROLE_LOG_READER") || a.getAuthority().equals("SCOPE_log.read"));
    }
}
