package com.ragchat.user.model.dto.response;

import java.util.Map;

public record ErrorDetails(String code, String message, String path, Map<String, String> details) {}
