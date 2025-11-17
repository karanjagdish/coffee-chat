package com.ragchat.chat.model.dto.response;

import com.ragchat.chat.model.enums.SessionDocumentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionDocumentResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        SessionDocumentStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
