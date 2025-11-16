package com.ragchat.user.controller;

import com.ragchat.user.model.dto.request.LoginRequest;
import com.ragchat.user.model.dto.request.RefreshTokenRequest;
import com.ragchat.user.model.dto.request.SignupRequest;
import com.ragchat.user.model.dto.response.ApiResponse;
import com.ragchat.user.model.dto.response.AuthResponse;
import com.ragchat.user.model.dto.response.UserResponse;
import com.ragchat.user.security.UserPrincipal;
import com.ragchat.user.service.AuthService;
import com.ragchat.user.service.JwtService;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username/email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Validate JWT token and return user info")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUserId();
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}
