package com.ragchat.user.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchat.user.config.UserServicePostgresTestConfig;
import com.ragchat.user.model.entity.User;
import com.ragchat.user.repository.UserRepository;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UserServicePostgresTestConfig.class)
@ExtendWith(SpringExtension.class)
class UserControllerIT {

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        UserServicePostgresTestConfig.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCurrentUser_returnsProfileForAuthenticatedUser() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder()
                .username("get-me-" + suffix)
                .email("get-me-" + suffix + "@example.com")
                .passwordHash("hash")
                .apiKey("api-key-" + suffix)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        MvcResult result = mockMvc.perform(get("/api/users/me").header("X-API-KEY", user.getApiKey()))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.path("data");
        assertEquals(user.getId().toString(), data.path("id").asText());
        assertEquals(user.getUsername(), data.path("username").asText());
        assertEquals(user.getEmail(), data.path("email").asText());
    }

    @Test
    void updateCurrentUser_updatesProfile() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder()
                .username("old-name-" + suffix)
                .email("old-" + suffix + "@example.com")
                .passwordHash("hash")
                .apiKey("api-key-" + suffix)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        String bodyJson = "{"
                + "\"username\":\"new-name-"
                + suffix
                + "\","
                + "\"email\":\"new-"
                + suffix
                + "@example.com\""
                + "}";

        mockMvc.perform(put("/api/users/me")
                        .header("X-API-KEY", user.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk());

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("new-name-" + suffix, reloaded.getUsername());
        assertEquals("new-" + suffix + "@example.com", reloaded.getEmail());
    }

    @Test
    void regenerateApiKey_generatesNewKey() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder()
                .username("key-user-" + suffix)
                .email("key-" + suffix + "@example.com")
                .passwordHash("hash")
                .apiKey("old-key-" + suffix)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        mockMvc.perform(post("/api/users/me/regenerate-api-key").header("X-API-KEY", user.getApiKey()))
                .andExpect(status().isOk());

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertNotEquals("old-key-" + suffix, reloaded.getApiKey());
    }

    @Test
    void getApiKey_returnsCurrentKey() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder()
                .username("api-user-" + suffix)
                .email("api-" + suffix + "@example.com")
                .passwordHash("hash")
                .apiKey("current-key-" + suffix)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        MvcResult result = mockMvc.perform(get("/api/users/me/api-key").header("X-API-KEY", user.getApiKey()))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.path("data");
        assertEquals("current-key-" + suffix, data.path("apiKey").asText());
    }

    @Nested
    class NegativeCases {

        @Test
        void getCurrentUser_withoutApiKey_returnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me")).andExpect(status().is4xxClientError());
        }

        @Test
        void getCurrentUser_withInvalidApiKey_returnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/users/me").header("X-API-KEY", "invalid-key"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void updateCurrentUser_withInvalidPayload_returnsBadRequestWithValidationErrors() throws Exception {
            String suffix = UUID.randomUUID().toString();
            User user = User.builder()
                    .username("valid-user-" + suffix)
                    .email("valid-" + suffix + "@example.com")
                    .passwordHash("hash")
                    .apiKey("api-key-" + suffix)
                    .isActive(true)
                    .build();

            user = userRepository.save(user);

            String bodyJson = "{"
                    + "\"username\":\"ab\","
                    + // too short
                    "\"email\":\"not-an-email\""
                    + // invalid email
                    "}";

            MvcResult result = mockMvc.perform(put("/api/users/me")
                            .header("X-API-KEY", user.getApiKey())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(json);
            assertFalse(root.path("success").asBoolean());
            JsonNode error = root.path("error");
            assertEquals("VALIDATION_FAILED", error.path("code").asText());
            JsonNode details = error.path("details");
            assertTrue(details.has("username"));
            assertTrue(details.has("email"));
        }
    }
}
