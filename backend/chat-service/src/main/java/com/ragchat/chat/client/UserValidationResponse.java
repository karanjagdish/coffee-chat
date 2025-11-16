package com.ragchat.chat.client;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserValidationResponse(UUID id, String username, String email, LocalDateTime createdAt) {}
