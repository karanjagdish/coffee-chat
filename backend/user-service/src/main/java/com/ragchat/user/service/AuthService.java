package com.ragchat.user.service;

import com.ragchat.user.exception.DuplicateResourceException;
import com.ragchat.user.model.dto.request.LoginRequest;
import com.ragchat.user.model.dto.request.SignupRequest;
import com.ragchat.user.model.dto.response.AuthResponse;
import com.ragchat.user.model.dto.response.UserResponse;
import com.ragchat.user.model.entity.User;
import com.ragchat.user.repository.UserRepository;
import com.ragchat.user.security.UserPrincipal;
import com.ragchat.user.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApiKeyGenerator apiKeyGenerator;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .apiKey(apiKeyGenerator.generateApiKey())
                .isActive(true)
                .build();

        user = userRepository.save(user);

        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getIsActive());

        String token = jwtService.generateToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        UserResponse userResponse =
                new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());

        return new AuthResponse(token, refreshToken, user.getApiKey(), userResponse, jwtService.getExpirationMs());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getIsActive());

        String token = jwtService.generateToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        UserResponse userResponse =
                new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());

        return new AuthResponse(token, refreshToken, user.getApiKey(), userResponse, jwtService.getExpirationMs());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getIsActive());

        String newToken = jwtService.generateToken(userPrincipal);
        String newRefreshToken = jwtService.generateRefreshToken(userPrincipal);

        UserResponse userResponse =
                new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());

        return new AuthResponse(
                newToken, newRefreshToken, user.getApiKey(), userResponse, jwtService.getExpirationMs());
    }
}
