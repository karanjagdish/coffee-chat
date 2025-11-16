package com.ragchat.user.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchat.user.config.UserServicePostgresTestConfig;
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
class AuthControllerIT {

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        UserServicePostgresTestConfig.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signup_login_refresh_and_validateToken_flow() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = "ituser_" + suffix;
        String email = "ituser_" + suffix + "@example.com";
        String password = "Password123!";

        String signupBody = "{"
                + "\"username\":\""
                + username
                + "\","
                + "\"email\":\""
                + email
                + "\","
                + "\"password\":\""
                + password
                + "\""
                + "}";

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isOk())
                .andReturn();

        String signupJson = signupResult.getResponse().getContentAsString();
        JsonNode signupRoot = objectMapper.readTree(signupJson);
        JsonNode signupData = signupRoot.path("data");
        String signupToken = signupData.path("token").asText();
        String signupRefreshToken = signupData.path("refreshToken").asText();

        assertFalse(signupToken.isEmpty());
        assertFalse(signupRefreshToken.isEmpty());

        String loginBody =
                "{" + "\"usernameOrEmail\":\"" + username + "\"," + "\"password\":\"" + password + "\"" + "}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String loginJson = loginResult.getResponse().getContentAsString();
        JsonNode loginRoot = objectMapper.readTree(loginJson);
        JsonNode loginData = loginRoot.path("data");
        String loginToken = loginData.path("token").asText();
        String loginRefreshToken = loginData.path("refreshToken").asText();

        assertFalse(loginToken.isEmpty());
        assertFalse(loginRefreshToken.isEmpty());

        String refreshBody = "{" + "\"refreshToken\":\"" + loginRefreshToken + "\"" + "}";

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        String refreshJson = refreshResult.getResponse().getContentAsString();
        JsonNode refreshRoot = objectMapper.readTree(refreshJson);
        JsonNode refreshData = refreshRoot.path("data");
        String newToken = refreshData.path("token").asText();

        assertFalse(newToken.isEmpty());

        MvcResult validateResult = mockMvc.perform(
                        get("/api/auth/validate-token").header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andReturn();

        String validateJson = validateResult.getResponse().getContentAsString();
        JsonNode validateRoot = objectMapper.readTree(validateJson);
        JsonNode validateData = validateRoot.path("data");
        assertEquals(username, validateData.path("username").asText());
        assertEquals(email, validateData.path("email").asText());
    }

    @Nested
    class NegativeCases {

        @Test
        void signup_withInvalidPayload_returnsBadRequestWithValidationErrors() throws Exception {
            String body = "{"
                    + "\"username\":\"ab\","
                    + // too short
                    "\"email\":\"not-an-email\","
                    + // invalid email
                    "\"password\":\"short\""
                    + // too short
                    "}";

            MvcResult result = mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
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
            assertTrue(details.has("password"));
        }

        @Test
        void login_withWrongPassword_returnsUnauthorized() throws Exception {
            String suffix = UUID.randomUUID().toString().replace("-", "");
            String username = "neguser_" + suffix;
            String email = "neguser_" + suffix + "@example.com";
            String password = "Password123!";

            String signupBody = "{"
                    + "\"username\":\""
                    + username
                    + "\","
                    + "\"email\":\""
                    + email
                    + "\","
                    + "\"password\":\""
                    + password
                    + "\""
                    + "}";

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody))
                    .andExpect(status().isOk());

            String loginBody =
                    "{" + "\"usernameOrEmail\":\"" + username + "\"," + "\"password\":\"wrong-password\"" + "}";

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(json);
            assertFalse(root.path("success").asBoolean());
            assertEquals(
                    "AUTHENTICATION_FAILED", root.path("error").path("code").asText());
        }

        @Test
        void refresh_withInvalidToken_returnsUnauthorized() throws Exception {
            String body = "{" + "\"refreshToken\":\"invalid-token\"" + "}";

            MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            String json = result.getResponse().getContentAsString();
            JsonNode root = objectMapper.readTree(json);
            assertFalse(root.path("success").asBoolean());
            assertEquals(
                    "AUTHENTICATION_FAILED", root.path("error").path("code").asText());
        }
    }
}
