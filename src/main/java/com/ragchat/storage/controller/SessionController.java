package com.ragchat.storage.controller;

import com.ragchat.storage.dto.SessionRequest;
import com.ragchat.storage.dto.SessionResponse;
import com.ragchat.storage.service.RateLimitService;
import com.ragchat.storage.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sessions", description = "Chat session management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@SecurityRequirement(name = "API Key")
public class SessionController {

    private final SessionService sessionService;
    private final RateLimitService rateLimitService;

    @PostMapping
    @Operation(summary = "Create a new chat session", description = "Creates a new chat session for the authenticated user")
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody SessionRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        log.info("Creating session for user: {}", userId);
        SessionResponse response = sessionService.createSession(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all sessions", description = "Retrieves all chat sessions for the authenticated user with pagination")
    public ResponseEntity<Page<SessionResponse>> getAllSessions(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "updatedAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<SessionResponse> sessions = sessionService.getAllSessions(userId, pageable);

        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite sessions", description = "Retrieves all favorite chat sessions for the authenticated user")
    public ResponseEntity<Page<SessionResponse>> getFavoriteSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<SessionResponse> sessions = sessionService.getFavoriteSessions(userId, pageable);

        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID", description = "Retrieves a specific chat session by ID")
    public ResponseEntity<SessionResponse> getSessionById(
            @PathVariable Long id,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        SessionResponse response = sessionService.getSessionById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update session name", description = "Updates the name of a chat session")
    public ResponseEntity<SessionResponse> updateSessionName(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        SessionResponse response = sessionService.updateSessionName(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite status", description = "Toggles the favorite status of a chat session")
    public ResponseEntity<SessionResponse> toggleFavorite(
            @PathVariable Long id,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        SessionResponse response = sessionService.toggleFavorite(id, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete session", description = "Deletes a chat session and all its messages")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long id,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        sessionService.deleteSession(id, userId);
        return ResponseEntity.noContent().build();
    }
}