package com.ragchat.storage.repository;

import com.ragchat.storage.model.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<ChatSession, Long> {
    Page<ChatSession> findByUserId(String userId, Pageable pageable);
    Page<ChatSession> findByUserIdAndIsFavorite(String userId, Boolean isFavorite, Pageable pageable);
    Optional<ChatSession> findByIdAndUserId(Long id, String userId);
}