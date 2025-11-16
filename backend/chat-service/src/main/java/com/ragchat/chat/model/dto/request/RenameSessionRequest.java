package com.ragchat.chat.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameSessionRequest(
        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must not exceed 255 characters")
                String name) {}
