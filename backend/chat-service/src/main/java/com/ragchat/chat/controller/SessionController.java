package com.ragchat.chat.controller;

import com.ragchat.chat.model.dto.request.CreateSessionRequest;
import com.ragchat.chat.model.dto.request.RenameSessionRequest;
import com.ragchat.chat.model.dto.response.ApiResponse;
import com.ragchat.chat.model.dto.response.SessionResponse;
import com.ragchat.chat.security.ChatUserPrincipal;
import com.ragchat.chat.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "Chat Sessions", description = "Chat session management endpoints")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "Create a new chat session")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @AuthenticationPrincipal ChatUserPrincipal principal, @Valid @RequestBody CreateSessionRequest request) {
        UUID userId = principal.getUserId();
        SessionResponse response = sessionService.createSession(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List chat sessions for current user")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @AuthenticationPrincipal ChatUserPrincipal principal) {
        UUID userId = principal.getUserId();
        List<SessionResponse> sessions = sessionService.getSessions(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a chat session by id")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @AuthenticationPrincipal ChatUserPrincipal principal, @PathVariable("id") UUID id) {
        UUID userId = principal.getUserId();
        SessionResponse session = sessionService.getSession(userId, id);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PatchMapping("/{id}/rename")
    @Operation(summary = "Rename a chat session")
    public ResponseEntity<ApiResponse<SessionResponse>> renameSession(
            @AuthenticationPrincipal ChatUserPrincipal principal,
            @PathVariable("id") UUID id,
            @Valid @RequestBody RenameSessionRequest request) {
        UUID userId = principal.getUserId();
        SessionResponse session = sessionService.renameSession(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @PatchMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite status for a chat session")
    public ResponseEntity<ApiResponse<SessionResponse>> toggleFavorite(
            @AuthenticationPrincipal ChatUserPrincipal principal, @PathVariable("id") UUID id) {
        UUID userId = principal.getUserId();
        SessionResponse session = sessionService.toggleFavorite(userId, id);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a chat session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal ChatUserPrincipal principal, @PathVariable("id") UUID id) {
        UUID userId = principal.getUserId();
        sessionService.deleteSession(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
