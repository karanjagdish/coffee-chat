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
import java.time.LocalDateTime;
import java.util.UUID;
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
class SessionControllerIT {

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        ChatServicePostgresTestConfig.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @Test
    void createAndListSessions_forAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userServiceClient.validateToken("valid-token"))
                .thenReturn(
                        new UserValidationResponse(userId, "chat-it-user", "chat@example.com", LocalDateTime.now()));

        String bodyJson = "{" + "\"sessionName\":\"IT Session\"" + "}";

        MvcResult createResult = mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andReturn();

        String createJson = createResult.getResponse().getContentAsString();
        JsonNode createRoot = objectMapper.readTree(createJson);
        JsonNode createData = createRoot.path("data");
        String sessionId = createData.path("id").asText();
        assertFalse(sessionId.isEmpty());
        assertEquals("IT Session", createData.path("sessionName").asText());

        MvcResult listResult = mockMvc.perform(get("/api/sessions").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn();

        String listJson = listResult.getResponse().getContentAsString();
        JsonNode listRoot = objectMapper.readTree(listJson);
        JsonNode listData = listRoot.path("data");
        assertTrue(listData.isArray());
        assertTrue(listData.size() >= 1);

        JsonNode first = listData.get(0);
        assertEquals("IT Session", first.path("sessionName").asText());
    }

    @Nested
    class NegativeCases {

        @Test
        void createSession_withoutAuthorization_returnsClientError() throws Exception {
            String bodyJson = "{" + "\"sessionName\":\"No Auth\"" + "}";

            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void createSession_withInvalidToken_returnsClientError() throws Exception {
            when(userServiceClient.validateToken("invalid-token"))
                    .thenThrow(new IllegalStateException("Invalid token"));

            String bodyJson = "{" + "\"sessionName\":\"Invalid Token\"" + "}";

            mockMvc.perform(post("/api/sessions")
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void createSession_withInvalidPayload_returnsBadRequestWithValidationErrors() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userServiceClient.validateToken("valid-token"))
                    .thenReturn(new UserValidationResponse(
                            userId, "chat-it-user", "chat@example.com", LocalDateTime.now()));

            String bodyJson = "{"
                    + "\"sessionName\":\"\""
                    + // blank name
                    "}";

            mockMvc.perform(post("/api/sessions")
                            .header("Authorization", "Bearer valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    class RateLimitCases {

        @Test
        void listSessions_hitsRateLimitAfterSeveralRequests() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userServiceClient.validateToken("rate-token"))
                    .thenReturn(
                            new UserValidationResponse(userId, "rate-user", "rate@example.com", LocalDateTime.now()));

            // Make a few requests; only assert that a later one is rate-limited
            mockMvc.perform(get("/api/sessions").header("Authorization", "Bearer rate-token"));

            mockMvc.perform(get("/api/sessions").header("Authorization", "Bearer rate-token"));

            mockMvc.perform(get("/api/sessions").header("Authorization", "Bearer rate-token"))
                    .andExpect(status().isTooManyRequests());
        }
    }
}
