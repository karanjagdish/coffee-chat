package com.ragchat.chat.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank(message = "Session name is required")
                @Size(max = 255, message = "Session name must not exceed 255 characters")
                String sessionName) {}
