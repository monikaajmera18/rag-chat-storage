package com.ragchat.storage.controller;

import com.ragchat.storage.dto.MessageRequest;
import com.ragchat.storage.dto.MessageResponse;
import com.ragchat.storage.service.MessageService;
import com.ragchat.storage.service.RateLimitService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions/{sessionId}/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "Chat message management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@SecurityRequirement(name = "API Key")
public class MessageController {

    private final MessageService messageService;
    private final RateLimitService rateLimitService;

    @PostMapping
    @Operation(summary = "Add a message", description = "Adds a new message to a chat session")
    public ResponseEntity<List<MessageResponse>> addMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody MessageRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        log.info("Adding message to session {} by user: {}", sessionId, userId);
        List<MessageResponse> response = messageService.addMessage(sessionId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get messages", description = "Retrieves all messages from a chat session with pagination")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable Long sessionId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            Authentication authentication) {

        String userId = authentication.getName();
        rateLimitService.checkRateLimit(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "timestamp"));
        Map<String, Object> response = messageService.getMessages(sessionId, userId, pageable);
        return ResponseEntity.ok(response);
    }
}
