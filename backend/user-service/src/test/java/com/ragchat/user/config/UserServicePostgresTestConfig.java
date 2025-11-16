package com.ragchat.user.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class UserServicePostgresTestConfig {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("user_service_it")
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
        registry.add("USER_DB_SCHEMA", () -> "public");
        // Test JWT secret generated via `openssl rand -base64 32`
        registry.add("JWT_SECRET", () -> "mxiRw4s11DY+3hpT07UDzIQAAa7n5b2u5nG4Y4ZyMnE=");
    }
}
