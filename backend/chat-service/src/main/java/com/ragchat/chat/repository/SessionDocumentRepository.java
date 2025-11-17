package com.ragchat.chat.repository;

import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.model.entity.SessionDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionDocumentRepository extends JpaRepository<SessionDocument, UUID> {

    List<SessionDocument> findBySessionOrderByCreatedAtDesc(ChatSession session);

    Optional<SessionDocument> findByIdAndSession(UUID id, ChatSession session);
}
