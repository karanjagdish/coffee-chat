package com.ragchat.chat.service;

import com.ragchat.chat.exception.ResourceNotFoundException;
import com.ragchat.chat.model.dto.request.CreateSessionRequest;
import com.ragchat.chat.model.dto.request.RenameSessionRequest;
import com.ragchat.chat.model.dto.response.SessionResponse;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.repository.ChatMessageRepository;
import com.ragchat.chat.repository.ChatSessionRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public SessionResponse createSession(UUID userId, CreateSessionRequest request) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName(request.sessionName())
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);
        return toResponse(session, 0);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(UUID userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream()
                .map(session -> toResponse(session, (int) chatMessageRepository.countBySession(session)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID userId, UUID sessionId) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        int messageCount = (int) chatMessageRepository.countBySession(session);
        return toResponse(session, messageCount);
    }

    @Transactional
    public SessionResponse renameSession(UUID userId, UUID sessionId, RenameSessionRequest request) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setSessionName(request.name());
        session = chatSessionRepository.save(session);
        int messageCount = (int) chatMessageRepository.countBySession(session);
        return toResponse(session, messageCount);
    }

    @Transactional
    public SessionResponse toggleFavorite(UUID userId, UUID sessionId) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setFavorite(!session.isFavorite());
        session = chatSessionRepository.save(session);
        int messageCount = (int) chatMessageRepository.countBySession(session);
        return toResponse(session, messageCount);
    }

    @Transactional
    public void deleteSession(UUID userId, UUID sessionId) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        chatSessionRepository.delete(session);
    }

    private SessionResponse toResponse(ChatSession session, int messageCount) {
        return new SessionResponse(
                session.getId(),
                session.getSessionName(),
                session.isFavorite(),
                messageCount,
                session.getCreatedAt(),
                session.getUpdatedAt());
    }
}
