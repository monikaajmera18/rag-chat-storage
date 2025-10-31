package com.ragchat.storage.service;

import com.ragchat.storage.dto.SessionRequest;
import com.ragchat.storage.dto.SessionResponse;
import com.ragchat.storage.exception.SessionNotFoundException;
import com.ragchat.storage.model.ChatSession;
import com.ragchat.storage.repository.MessageRepository;
import com.ragchat.storage.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ModelMapper modelMapper;

    @Transactional
    public SessionResponse createSession(String userId, SessionRequest request) {
        log.info("Creating new session for user: {}", userId);

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName(request.getSessionName())
                .isFavorite(false)
                .build();

        session = sessionRepository.save(session);

        kafkaProducerService.publishSessionEvent("SESSION_CREATED", session.getId(),
                                                  userId, session.getSessionName());

        return mapToResponse(session);
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> getAllSessions(String userId, Pageable pageable) {
        log.info("Fetching all sessions for user: {}", userId);

        Page<ChatSession> sessions = sessionRepository.findByUserId(userId, pageable);
        return sessions.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> getFavoriteSessions(String userId, Pageable pageable) {
        log.info("Fetching favorite sessions for user: {}", userId);

        Page<ChatSession> sessions = sessionRepository.findByUserIdAndIsFavorite(userId, true, pageable);
        return sessions.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sessions", key = "#sessionId")
    public SessionResponse getSessionById(Long sessionId, String userId) {
        log.info("Fetching session {} for user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));

        return mapToResponse(session);
    }

    @Transactional
    @CacheEvict(value = "sessions", key = "#sessionId")
    public SessionResponse updateSessionName(Long sessionId, String userId, SessionRequest request) {
        log.info("Updating session name for session {} by user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));

        session.setSessionName(request.getSessionName());
        session = sessionRepository.save(session);

        kafkaProducerService.publishSessionEvent("SESSION_RENAMED", session.getId(),
                                                  userId, session.getSessionName());

        return mapToResponse(session);
    }

    @Transactional
    @CacheEvict(value = "sessions", key = "#sessionId")
    public SessionResponse toggleFavorite(Long sessionId, String userId) {
        log.info("Toggling favorite status for session {} by user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));

        session.setIsFavorite(!session.getIsFavorite());
        session = sessionRepository.save(session);

        String eventType = session.getIsFavorite() ? "SESSION_FAVORITED" : "SESSION_UNFAVORITED";
        kafkaProducerService.publishSessionEvent(eventType, session.getId(),
                                                  userId, session.getSessionName());

        return mapToResponse(session);
    }

    @Transactional
    @CacheEvict(value = "sessions", key = "#sessionId")
    public void deleteSession(Long sessionId, String userId) {
        log.info("Deleting session {} by user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));

        sessionRepository.delete(session);

        kafkaProducerService.publishSessionEvent("SESSION_DELETED", sessionId,
                                                  userId, session.getSessionName());
    }

    private SessionResponse mapToResponse(ChatSession session) {
        SessionResponse response = modelMapper.map(session, SessionResponse.class);
        int messageCount = (int) messageRepository.countBySessionId(session.getId());
        response.setMessageCount(messageCount);
        return response;
    }
}
