package com.ragchat.chat.controller;

import com.ragchat.chat.model.dto.response.ApiResponse;
import com.ragchat.chat.model.dto.response.SessionDocumentResponse;
import com.ragchat.chat.security.ChatUserPrincipal;
import com.ragchat.chat.service.SessionDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sessions/{sessionId}/documents")
@RequiredArgsConstructor
@Tag(name = "Session Documents", description = "Upload and manage documents attached to chat sessions")
public class SessionDocumentController {

    private final SessionDocumentService sessionDocumentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document for a chat session")
    public ResponseEntity<ApiResponse<SessionDocumentResponse>> uploadDocument(
            @AuthenticationPrincipal ChatUserPrincipal principal,
            @PathVariable("sessionId") UUID sessionId,
            @RequestPart("file") MultipartFile file) {
        UUID userId = principal.getUserId();
        SessionDocumentResponse response = sessionDocumentService.uploadDocument(userId, sessionId, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List documents for a chat session")
    public ResponseEntity<ApiResponse<List<SessionDocumentResponse>>> listDocuments(
            @AuthenticationPrincipal ChatUserPrincipal principal, @PathVariable("sessionId") UUID sessionId) {
        UUID userId = principal.getUserId();
        List<SessionDocumentResponse> docs = sessionDocumentService.getDocuments(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document from a chat session")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal ChatUserPrincipal principal,
            @PathVariable("sessionId") UUID sessionId,
            @PathVariable("documentId") UUID documentId) {
        UUID userId = principal.getUserId();
        sessionDocumentService.deleteDocument(userId, sessionId, documentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
