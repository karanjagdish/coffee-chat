package com.ragchat.user.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters") String username,
        @Email(message = "Email must be valid") String email) {}
