package com.ragchat.chat.model.dto.request;

import com.ragchat.chat.model.enums.MessageSender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateMessageRequest(
        @NotNull(message = "Sender is required") MessageSender sender,
        @NotBlank(message = "Content is required") String content,
        Map<String, Object> context) {}
