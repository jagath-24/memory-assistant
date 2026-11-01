package com.memoryassistant.repository;

import com.memoryassistant.entity.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
    List<ConversationSession> findAllByOrderByCreatedAtDesc();
    List<ConversationSession> findByTitle(String title);
}
