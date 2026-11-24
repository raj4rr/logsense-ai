package com.logsense.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class SourceAuthorizationService {
    private final SecureMcpAuthorizationPort authorizationPort;
    public SourceAuthorizationService(SecureMcpAuthorizationPort authorizationPort) { this.authorizationPort = authorizationPort; }
    public void requireReadAccess(String connectorType, String source) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String tenantId = auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getClaimAsString("tenant_id") : null;
        if (source == null || source.isBlank() || connectorType == null || connectorType.isBlank())
            throw new AccessDeniedException("A source and connector type are required");
        if (!authorizationPort.canRead(auth, tenantId, connectorType, source))
            throw new AccessDeniedException("Missing log.read permission for this source and tenant");
    }
}
