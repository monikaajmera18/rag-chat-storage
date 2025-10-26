package com.ragchat.storage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchat.storage.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.security.api-key}")
    private String validApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip API key validation for public endpoints
        String path = request.getRequestURI();

        // UPDATED: Add /api/auth/** to skip list
        if (path.startsWith("/actuator/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/api-docs") ||
                path.equals("/") ||
                path.equals("/error") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars/") ||
                path.startsWith("/api/auth/")) {  // NEW: Skip auth endpoints

            log.debug("Skipping API key validation for public endpoint: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey == null || !apiKey.equals(validApiKey)) {
            log.warn("Invalid or missing API key from IP: {} for path: {}",
                    request.getRemoteAddr(), path);

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");

            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error("Unauthorized")
                    .message("Invalid or missing API key")
                    .path(request.getRequestURI())
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        log.debug("API key validated successfully for path: {}", path);
        filterChain.doFilter(request, response);
    }
}