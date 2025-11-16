package com.ragchat.chat.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ragchat.chat.config.ChatServicePostgresTestConfig;
import com.ragchat.chat.model.dto.request.CreateSessionRequest;
import com.ragchat.chat.model.dto.request.RenameSessionRequest;
import com.ragchat.chat.model.dto.response.SessionResponse;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.repository.ChatMessageRepository;
import com.ragchat.chat.repository.ChatSessionRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@Import(ChatServicePostgresTestConfig.class)
@ExtendWith(SpringExtension.class)
class SessionServiceIT {

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        ChatServicePostgresTestConfig.registerProperties(registry);
    }

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void createAndFetchSession_persistsToDatabase() {
        UUID userId = UUID.randomUUID();

        SessionResponse created = sessionService.createSession(userId, new CreateSessionRequest("IT Session"));

        ChatSession stored = chatSessionRepository.findById(created.id()).orElseThrow();
        assertEquals("IT Session", stored.getSessionName());
        assertEquals(userId, stored.getUserId());

        SessionResponse loaded = sessionService.getSession(userId, created.id());
        assertEquals(created.id(), loaded.id());
        assertEquals("IT Session", loaded.sessionName());
        assertEquals(0, loaded.messageCount());
    }

    @Test
    void renameAndToggleFavorite_arePersisted() {
        UUID userId = UUID.randomUUID();
        SessionResponse created = sessionService.createSession(userId, new CreateSessionRequest("Original"));

        SessionResponse renamed =
                sessionService.renameSession(userId, created.id(), new RenameSessionRequest("Renamed"));
        assertEquals("Renamed", renamed.sessionName());

        SessionResponse favorited = sessionService.toggleFavorite(userId, created.id());
        assertTrue(favorited.isFavorite());

        ChatSession stored = chatSessionRepository.findById(created.id()).orElseThrow();
        assertEquals("Renamed", stored.getSessionName());
        assertTrue(stored.isFavorite());
    }

    @Test
    void deleteSession_removesFromDatabase() {
        UUID userId = UUID.randomUUID();
        SessionResponse created = sessionService.createSession(userId, new CreateSessionRequest("To Delete"));

        sessionService.deleteSession(userId, created.id());

        assertTrue(chatSessionRepository.findById(created.id()).isEmpty());
    }
}
