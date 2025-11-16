package com.ragchat.chat.repository;

import com.ragchat.chat.model.entity.ChatMessage;
import com.ragchat.chat.model.entity.ChatSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySessionOrderByMessageOrderAsc(ChatSession session);

    Page<ChatMessage> findBySessionOrderByMessageOrderDesc(ChatSession session, Pageable pageable);

    long countBySession(ChatSession session);

    Optional<ChatMessage> findTopBySessionOrderByMessageOrderDesc(ChatSession session);
}
