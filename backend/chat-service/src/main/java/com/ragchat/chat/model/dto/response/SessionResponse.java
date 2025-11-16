package com.ragchat.chat.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String sessionName,
        Boolean isFavorite,
        Integer messageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
