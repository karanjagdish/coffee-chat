package com.ragchat.chat.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchat.chat.client.UserServiceClient;
import com.ragchat.chat.client.UserValidationResponse;
import com.ragchat.chat.config.ChatServicePostgresTestConfig;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.model.entity.SessionDocument;
import com.ragchat.chat.repository.ChatSessionRepository;
import com.ragchat.chat.repository.SessionDocumentRepository;
import io.github.bucket4j.Bucket;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ChatServicePostgresTestConfig.class)
@ExtendWith(SpringExtension.class)
class SessionDocumentControllerIT {

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        ChatServicePostgresTestConfig.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private SessionDocumentRepository sessionDocumentRepository;

    @Autowired
    private Map<String, Bucket> rateLimitBuckets;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        rateLimitBuckets.clear();
        sessionDocumentRepository.deleteAll();
        chatSessionRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        sessionDocumentRepository.deleteAll();
        chatSessionRepository.deleteAll();
        cleanStorageDir();
    }

    @Test
    void uploadDocument_persistsMetadataAndReturnsResponse() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.validateToken("valid-token"))
                .thenReturn(new UserValidationResponse(userId, "doc-it-user", "doc@example.com", LocalDateTime.now()));

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName("Session With Docs")
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "Hello from document".getBytes(StandardCharsets.UTF_8));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/sessions/" + session.getId() + "/documents")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn();

        String json = uploadResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.path("data");
        assertEquals("test.txt", data.path("filename").asText());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, data.path("contentType").asText());
        assertTrue(data.path("sizeBytes").asLong() > 0);

        List<SessionDocument> stored = sessionDocumentRepository.findBySessionOrderByCreatedAtDesc(session);
        assertEquals(1, stored.size());
        SessionDocument doc = stored.get(0);
        assertEquals("test.txt", doc.getOriginalFilename());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, doc.getContentType());
        assertTrue(doc.getSizeBytes() > 0);
        assertNotNull(doc.getStoragePath());
    }

    @Test
    void listDocuments_returnsUploadedDocuments() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.validateToken("valid-token"))
                .thenReturn(new UserValidationResponse(userId, "doc-it-user", "doc@example.com", LocalDateTime.now()));

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName("Session With Docs")
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);

        SessionDocument document = SessionDocument.builder()
                .session(session)
                .originalFilename("test-list.txt")
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .sizeBytes(123L)
                .storagePath("/tmp/test-list.txt")
                .indexingStatus(com.ragchat.chat.model.enums.SessionDocumentStatus.READY)
                .errorMessage(null)
                .build();

        sessionDocumentRepository.save(document);

        MvcResult listResult = mockMvc.perform(get("/api/sessions/" + session.getId() + "/documents")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn();

        String json = listResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.path("data");
        assertTrue(data.isArray());
        assertTrue(data.size() >= 1);
        JsonNode first = data.get(0);
        assertEquals("test-list.txt", first.path("filename").asText());
    }

    @Test
    void deleteDocument_removesDocument() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.validateToken("valid-token"))
                .thenReturn(new UserValidationResponse(userId, "doc-it-user", "doc@example.com", LocalDateTime.now()));

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName("Session With Docs")
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);

        SessionDocument document = SessionDocument.builder()
                .session(session)
                .originalFilename("test-delete.txt")
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .sizeBytes(456L)
                .storagePath("/tmp/test-delete.txt")
                .indexingStatus(com.ragchat.chat.model.enums.SessionDocumentStatus.READY)
                .errorMessage(null)
                .build();

        document = sessionDocumentRepository.save(document);

        mockMvc.perform(delete("/api/sessions/" + session.getId() + "/documents/" + document.getId())
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        List<SessionDocument> remaining = sessionDocumentRepository.findBySessionOrderByCreatedAtDesc(session);
        assertEquals(0, remaining.size());
    }

    private void cleanStorageDir() {
        Path root = Paths.get("storage", "session-docs");
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
