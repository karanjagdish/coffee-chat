package com.ragchat.user.controller;

import com.ragchat.user.model.dto.request.UpdateUserRequest;
import com.ragchat.user.model.dto.response.ApiKeyResponse;
import com.ragchat.user.model.dto.response.ApiResponse;
import com.ragchat.user.model.dto.response.UserResponse;
import com.ragchat.user.security.UserPrincipal;
import com.ragchat.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User profile and API key management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUserId();
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            Authentication authentication, @Valid @RequestBody UpdateUserRequest request) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUserId();
        UserResponse userResponse = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @PostMapping("/me/regenerate-api-key")
    @Operation(summary = "Regenerate API key for current user")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> regenerateApiKey(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUserId();
        ApiKeyResponse response = userService.regenerateApiKey(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me/api-key")
    @Operation(summary = "Get current user's API key")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> getApiKey(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUserId();
        ApiKeyResponse response = userService.getApiKey(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
