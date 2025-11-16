package com.ragchat.user.service;

import com.ragchat.user.exception.ResourceNotFoundException;
import com.ragchat.user.model.dto.request.UpdateUserRequest;
import com.ragchat.user.model.dto.response.ApiKeyResponse;
import com.ragchat.user.model.dto.response.UserResponse;
import com.ragchat.user.model.entity.User;
import com.ragchat.user.repository.UserRepository;
import com.ragchat.user.security.UserPrincipal;
import com.ragchat.user.util.ApiKeyGenerator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new UserPrincipal(
                user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getIsActive());
    }

    public UserDetails loadUserByApiKey(String apiKey) {
        User user =
                userRepository.findByApiKey(apiKey).orElseThrow(() -> new UsernameNotFoundException("Invalid API key"));

        return new UserPrincipal(
                user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getIsActive());
    }

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Username already taken");
            }
            user.setUsername(request.username());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already taken");
            }
            user.setEmail(request.email());
        }

        user = userRepository.save(user);
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }

    @Transactional
    public ApiKeyResponse regenerateApiKey(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newApiKey = apiKeyGenerator.generateApiKey();
        user.setApiKey(newApiKey);
        userRepository.save(user);

        return new ApiKeyResponse(newApiKey);
    }

    public ApiKeyResponse getApiKey(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new ApiKeyResponse(user.getApiKey());
    }
}
