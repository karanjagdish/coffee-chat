package com.ragchat.chat.model.dto.response;

import com.ragchat.chat.model.enums.MessageSender;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        MessageSender sender,
        String content,
        Map<String, Object> context,
        Integer messageOrder,
        LocalDateTime createdAt) {}
