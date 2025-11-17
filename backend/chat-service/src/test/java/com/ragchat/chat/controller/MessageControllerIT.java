package com.ragchat.chat.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchat.chat.client.UserServiceClient;
import com.ragchat.chat.client.UserValidationResponse;
import com.ragchat.chat.config.ChatServicePostgresTestConfig;
import com.ragchat.chat.model.entity.ChatMessage;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.model.enums.MessageSender;
import com.ragchat.chat.repository.ChatMessageRepository;
import com.ragchat.chat.repository.ChatSessionRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
class MessageControllerIT {

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
    private ChatMessageRepository chatMessageRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        chatSessionRepository.deleteAll();
        chatMessageRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        chatSessionRepository.deleteAll();
        chatMessageRepository.deleteAll();
    }

    @Test
    void createMessage_persistsToDatabase() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.validateToken("valid-token"))
                .thenReturn(
                        new UserValidationResponse(userId, "chat-it-user", "chat@example.com", LocalDateTime.now()));

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName("Session With Messages")
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);

        String messageBody =
                "{" + "\"sender\":\"" + MessageSender.USER.name() + "\"," + "\"content\":\"Hello from chat IT\"" + "}";

        MvcResult createResult = mockMvc.perform(post("/api/sessions/" + session.getId() + "/messages")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageBody))
                .andExpect(status().isOk())
                .andReturn();

        String createJson = createResult.getResponse().getContentAsString();
        JsonNode createRoot = objectMapper.readTree(createJson);
        JsonNode createData = createRoot.path("data");
        assertEquals("Hello from chat IT", createData.path("content").asText());

        // Verify persisted in DB
        var stored = chatMessageRepository.findBySessionOrderByMessageOrderAsc(session);
        assertEquals(2, stored.size());
        ChatMessage first = stored.get(0);
        assertEquals(MessageSender.USER, first.getSender());
        assertEquals("Hello from chat IT", first.getContent());
    }

    @Test
    void getMessages_returnsStoredMessages() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.validateToken("valid-token"))
                .thenReturn(
                        new UserValidationResponse(userId, "chat-it-user", "chat@example.com", LocalDateTime.now()));

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName("Session With Messages")
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);

        ChatMessage first = ChatMessage.builder()
                .session(session)
                .sender(MessageSender.USER)
                .content("First message")
                .messageOrder(1)
                .build();

        ChatMessage second = ChatMessage.builder()
                .session(session)
                .sender(MessageSender.AI)
                .content("Second message")
                .messageOrder(2)
                .build();

        chatMessageRepository.save(first);
        chatMessageRepository.save(second);

        MvcResult listResult = mockMvc.perform(get("/api/sessions/" + session.getId() + "/messages")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn();

        String listJson = listResult.getResponse().getContentAsString();
        JsonNode listRoot = objectMapper.readTree(listJson);
        JsonNode pageNode = listRoot.path("data");
        JsonNode contentNode = pageNode.path("content");

        assertTrue(contentNode.isArray());
        assertEquals(2, contentNode.size());
        assertEquals("Second message", contentNode.get(0).path("content").asText());
        assertEquals("First message", contentNode.get(1).path("content").asText());
    }

    @Nested
    class NegativeCases {

        @Test
        void createMessage_withoutAuthorization_returnsClientError() throws Exception {
            mockMvc.perform(post("/api/sessions/" + UUID.randomUUID() + "/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void createMessage_withInvalidToken_returnsClientError() throws Exception {
            when(userServiceClient.validateToken("invalid-token"))
                    .thenThrow(new IllegalStateException("Invalid token"));

            mockMvc.perform(post("/api/sessions/" + UUID.randomUUID() + "/messages")
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void createMessage_withInvalidPayload_returnsBadRequestWithValidationErrors() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userServiceClient.validateToken("valid-token"))
                    .thenReturn(new UserValidationResponse(
                            userId, "chat-it-user", "chat@example.com", LocalDateTime.now()));

            ChatSession session = ChatSession.builder()
                    .userId(userId)
                    .sessionName("Invalid Message Session")
                    .favorite(false)
                    .build();

            session = chatSessionRepository.save(session);

            // Valid sender but blank content to trigger @NotBlank validation
            String body = "{" + "\"sender\":\"" + MessageSender.USER.name() + "\"," + "\"content\":\"\"" + "}";

            mockMvc.perform(post("/api/sessions/" + session.getId() + "/messages")
                            .header("Authorization", "Bearer valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is4xxClientError());
        }
    }
}
