package com.logsense.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public org.springframework.http.ResponseEntity<Map<String,String>> badRequest(Exception ex) {
        return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", "Invalid query or request parameters"));
    }
    @ExceptionHandler({AccessDeniedException.class, SecurityException.class})
    public org.springframework.http.ResponseEntity<Map<String,String>> forbidden(Exception ex) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "FORBIDDEN", "message", "Access to the requested log source is not permitted"));
    }
    @ExceptionHandler(IllegalStateException.class)
    public org.springframework.http.ResponseEntity<Map<String,String>> upstream(Exception ex) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "SOURCE_UNAVAILABLE", "message", "A configured log source or AI provider is unavailable"));
    }
}
