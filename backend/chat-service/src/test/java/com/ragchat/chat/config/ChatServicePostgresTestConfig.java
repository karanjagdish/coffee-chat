package com.ragchat.chat.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class ChatServicePostgresTestConfig {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("chat_service_it")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/test-init.sql");

    static {
        POSTGRES.start();
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("SPRING_DATASOURCE_URL", POSTGRES::getJdbcUrl);
        registry.add("SPRING_DATASOURCE_USERNAME", POSTGRES::getUsername);
        registry.add("SPRING_DATASOURCE_PASSWORD", POSTGRES::getPassword);
        registry.add("CHAT_DB_SCHEMA", () -> "public");
        registry.add("JWT_SECRET", () -> "test-secret-test-secret-test-secret-1234");
        registry.add("USER_SERVICE_URL", () -> "http://localhost:8081/user");
        // Tight rate limit for tests so we can hit the limit quickly
        registry.add("rate-limit.capacity", () -> 2);
        registry.add("rate-limit.refill-tokens", () -> 2);
        registry.add("rate-limit.refill-duration-minutes", () -> 60);
    }
}
