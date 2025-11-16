package com.ragchat.chat.controller;

import com.ragchat.chat.model.dto.request.CreateMessageRequest;
import com.ragchat.chat.model.dto.response.ApiResponse;
import com.ragchat.chat.model.dto.response.MessageResponse;
import com.ragchat.chat.model.dto.response.PageResponse;
import com.ragchat.chat.security.ChatUserPrincipal;
import com.ragchat.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/messages")
@RequiredArgsConstructor
@Tag(name = "Chat Messages", description = "Chat message management endpoints")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "Create a new message in a chat session")
    public ResponseEntity<ApiResponse<MessageResponse>> createMessage(
            @AuthenticationPrincipal ChatUserPrincipal principal,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody CreateMessageRequest request) {
        UUID userId = principal.getUserId();
        MessageResponse response = messageService.createMessage(userId, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get messages for a chat session")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @AuthenticationPrincipal ChatUserPrincipal principal,
            @PathVariable("sessionId") UUID sessionId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        UUID userId = principal.getUserId();
        PageResponse<MessageResponse> pageResponse = messageService.getMessagesPage(userId, sessionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }
}
