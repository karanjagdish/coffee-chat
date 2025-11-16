package com.ragchat.user.model.dto.response;

public record AuthResponse(String token, String refreshToken, String apiKey, UserResponse user, long expiresIn) {}
