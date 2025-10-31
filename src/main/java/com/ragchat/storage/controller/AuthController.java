package com.ragchat.storage.controller;

import com.ragchat.storage.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "JWT token generation endpoints (Development/Testing only)")
public class AuthController {

    private final JwtUtil jwtUtil;

    /**
     * Generate JWT token via POST request
     *
     * @param request Map containing userId
     * @return JWT token and user information
     */
    @PostMapping("/generate-token")
    @Operation(
            summary = "Generate JWT Token (POST)",
            description = "Generates a JWT token for testing purposes. Send userId in request body."
    )
    public ResponseEntity<Void> generateToken(@RequestBody Map<String, String> request) {
        log.info("Token generation request received");

        String userId = request.getOrDefault("userId", "default-user");
        log.info("Generating token for user: {}", userId);

        String token = jwtUtil.generateToken(userId);
        log.info("Token is: {}", token);
        log.info("Token generated successfully for user: {}", userId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-JWT-Token", token);

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
    }

    /**
     * Test endpoint to verify token is valid
     *
     *
     * @return Validation result
     */
    @PostMapping("/validate-token")
    @Operation(
            summary = "Validate JWT Token",
            description = "Checks if a JWT token is valid"
    )
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        Map<String, Object> response = new HashMap<>();

        try {
            boolean isValid = jwtUtil.validateToken(token);
            String userId = jwtUtil.getUserIdFromToken(token);

            response.put("valid", isValid);
            response.put("userId", userId);
            response.put("message", "Token is valid");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
            response.put("message", "Token is invalid");

            return ResponseEntity.badRequest().body(response);
        }
    }
}