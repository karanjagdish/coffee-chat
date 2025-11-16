package com.ragchat.user.model.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;

@Builder
public record ApiResponse<T>(boolean success, T data, ErrorDetails error, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(new ErrorDetails(code, message, path, null))
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, String path, Map<String, String> details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(new ErrorDetails(code, message, path, details))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
