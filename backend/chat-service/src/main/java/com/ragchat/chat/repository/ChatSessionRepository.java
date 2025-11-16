package com.ragchat.chat.repository;

import com.ragchat.chat.model.entity.ChatSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);
}
