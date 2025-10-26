package com.ragchat.storage.repository;

import com.ragchat.storage.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findBySessionId(Long sessionId, Pageable pageable);
    long countBySessionId(Long sessionId);
}