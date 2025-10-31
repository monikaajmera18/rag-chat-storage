package com.ragchat.storage.service;

import com.ragchat.storage.dto.MessageRequest;
import com.ragchat.storage.dto.MessageResponse;
import com.ragchat.storage.exception.SessionNotFoundException;
import com.ragchat.storage.model.ChatMessage;
import com.ragchat.storage.model.ChatSession;
import com.ragchat.storage.model.SenderType;
import com.ragchat.storage.repository.MessageRepository;
import com.ragchat.storage.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;
    private final KafkaProducerService kafkaProducerService;
    private final HuggingFaceService huggingFaceService;
    private final ModelMapper modelMapper;

    @Transactional
    public List<MessageResponse> addMessage(Long sessionId, String userId, MessageRequest request) {
        log.info("Adding message to session {} by user: {}", sessionId, userId);
        long startTime = System.currentTimeMillis();
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));
        List<MessageResponse> responses = new ArrayList<>();

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .sender(SenderType.USER)
                .content(request.getContent())
                .context(request.getContext())
                .build();

        message = messageRepository.save(message);

        kafkaProducerService.publishMessageEvent("MESSAGE_ADDED", message.getId(),
                sessionId, SenderType.USER.toString(),
                request.getContent());

        log.info("User message saved with ID: {}", message.getId());

        responses.add(mapToResponse(message));

        // Generate AI response using Hugging Face
        Map<String, String> aiResponse = huggingFaceService.generateResponse(
                request.getContent(),
                request.getContext()
        );

        // Save AI response
        ChatMessage aiMessage = ChatMessage.builder()
                .session(session)
                .sender(SenderType.AI)
                .content(aiResponse.get("content"))
                .context(aiResponse.get("context"))
                .build();

        aiMessage = messageRepository.save(aiMessage);

        kafkaProducerService.publishMessageEvent("MESSAGE_ADDED", aiMessage.getId(),
                sessionId, SenderType.AI.toString(),
                aiResponse.get("content"));

        log.info("AI response saved with ID: {}", aiMessage.getId());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Chat completion finished in {} ms", processingTime);
        responses.add(mapToResponse(aiMessage));

        // Update session timestamp
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        return responses;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "messages", key = "#sessionId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Map<String, Object> getMessages(Long sessionId, String userId, Pageable pageable) {
        log.info("Fetching messages for session {} by user: {}", sessionId, userId);

        // Verify session belongs to user
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));

        Page<ChatMessage> messages = messageRepository.findBySessionId(sessionId, pageable);
        Page<MessageResponse> messagesPage = messages.map(this::mapToResponse);

        // Build cache-safe response map
        Map<String, Object> response = new HashMap<>();
        response.put("content", messagesPage.getContent());
        response.put("currentPage", messagesPage.getNumber());
        response.put("totalItems", messagesPage.getTotalElements());
        response.put("totalPages", messagesPage.getTotalPages());
        response.put("pageSize", messagesPage.getSize());
        response.put("hasNext", messagesPage.hasNext());
        response.put("hasPrevious", messagesPage.hasPrevious());
        response.put("isFirst", messagesPage.isFirst());
        response.put("isLast", messagesPage.isLast());

        log.debug("Returning {} messages for session {}", messagesPage.getContent().size(), sessionId);

        return response;
    }

    private MessageResponse mapToResponse(ChatMessage message) {
        return modelMapper.map(message, MessageResponse.class);
    }
}