package com.ragchat.storage.service;

import com.ragchat.storage.dto.MessageRequest;
import com.ragchat.storage.dto.MessageResponse;
import com.ragchat.storage.exception.SessionNotFoundException;
import com.ragchat.storage.model.ChatMessage;
import com.ragchat.storage.model.ChatSession;
import com.ragchat.storage.repository.MessageRepository;
import com.ragchat.storage.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public MessageResponse addMessage(Long sessionId, String userId, MessageRequest request) {
        log.info("Adding message to session {} by user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .sender(request.getSender())
                .content(request.getContent())
                .context(request.getContext())
                .build();

        message = messageRepository.save(message);

        kafkaProducerService.publishMessageEvent("MESSAGE_ADDED", message.getId(),
                                                  sessionId, request.getSender().toString(),
                                                  request.getContent());

        return mapToResponse(message);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "messages", key = "#sessionId + '_' + #pageable.pageNumber")
    public Page<MessageResponse> getMessages(Long sessionId, String userId, Pageable pageable) {
        log.info("Fetching messages for session {} by user: {}", sessionId, userId);

        // Verify session belongs to user
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));

        Page<ChatMessage> messages = messageRepository.findBySessionId(sessionId, pageable);
        return messages.map(this::mapToResponse);
    }

    private MessageResponse mapToResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .sender(message.getSender())
                .content(message.getContent())
                .context(message.getContext())
                .timestamp(message.getTimestamp())
                .build();
    }
}