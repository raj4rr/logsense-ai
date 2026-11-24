package com.logsense.analysis;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class LogRedactor {
    private static final Pattern AUTH = Pattern.compile("(?i)(authorization\\s*[:=]\\s*(?:bearer|basic)\\s+)[A-Za-z0-9._~+/=-]+");
    private static final Pattern SECRET = Pattern.compile("(?i)\\b(password|passwd|secret|api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret)\\s*[:=]\\s*[\\\"']?[^,\\s\\\"']+");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]+\\b");
    private static final Pattern AWS = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");
    private static final Pattern COOKIE = Pattern.compile("(?i)(set-cookie\\s*:\\s*)[^;\\r\\n]+");
    public String redact(String input) {
        if (input == null) return "";
        return COOKIE.matcher(AUTH.matcher(SECRET.matcher(JWT.matcher(AWS.matcher(input).replaceAll("[REDACTED_AWS_KEY]")).replaceAll("[REDACTED_JWT]")).replaceAll("$1=[REDACTED]")).replaceAll("$1[REDACTED]")).replaceAll("$1[REDACTED]");
    }
}
