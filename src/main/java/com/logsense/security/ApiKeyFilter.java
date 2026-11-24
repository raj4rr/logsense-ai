package com.logsense.security;

import com.logsense.config.LogSenseProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final LogSenseProperties properties;
    public ApiKeyFilter(LogSenseProperties properties) { this.properties = properties; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/actuator/health") || p.equals("/error");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String expected = properties.security() == null ? "" : properties.security().apiKey();
        String supplied = request.getHeader("X-API-Key");
        if (supplied == null) {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) supplied = auth.substring(7);
        }
        boolean localBypass = properties.security() != null && properties.security().allowLocalNoKey()
                && "local".equals(request.getParameter("profile"));
        boolean ok = expected != null && !expected.isBlank() && supplied != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
        if (ok) SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("api-key-client", "N/A", List.of(new SimpleGrantedAuthority("ROLE_LOG_READER"), new SimpleGrantedAuthority("ROLE_ADMIN"))));
        else if (!localBypass) { response.sendError(401, "Missing or invalid API key"); return; }
        chain.doFilter(request, response);
    }
}
