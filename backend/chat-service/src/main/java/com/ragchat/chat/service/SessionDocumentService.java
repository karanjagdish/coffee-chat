package com.ragchat.chat.service;

import com.ragchat.chat.exception.ResourceNotFoundException;
import com.ragchat.chat.model.dto.response.SessionDocumentResponse;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.model.entity.SessionDocument;
import com.ragchat.chat.model.enums.SessionDocumentStatus;
import com.ragchat.chat.repository.ChatSessionRepository;
import com.ragchat.chat.repository.SessionDocumentRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionDocumentService {

    private final ChatSessionRepository chatSessionRepository;
    private final SessionDocumentRepository sessionDocumentRepository;
    private final VectorStore vectorStore;

    private final Tika tika = new Tika();

    private static final Path STORAGE_ROOT = Paths.get("storage", "session-docs");

    @Transactional
    public SessionDocumentResponse uploadDocument(UUID userId, UUID sessionId, MultipartFile file) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        long sizeBytes = file.getSize();

        SessionDocument document = SessionDocument.builder()
                .session(session)
                .originalFilename(originalFilename)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .storagePath("")
                .indexingStatus(SessionDocumentStatus.PENDING)
                .errorMessage(null)
                .build();

        document = sessionDocumentRepository.save(document);

        Path sessionDir = STORAGE_ROOT.resolve(session.getId().toString());
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }

        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path targetPath = sessionDir.resolve(document.getId() + "-" + sanitizedFilename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath);
        } catch (IOException e) {
            log.error("Failed to save uploaded file", e);
            document.setIndexingStatus(SessionDocumentStatus.FAILED);
            document.setErrorMessage("Failed to save uploaded file");
            sessionDocumentRepository.save(document);
            throw new RuntimeException("Failed to save uploaded file", e);
        }

        document.setStoragePath(targetPath.toString());
        document.setIndexingStatus(SessionDocumentStatus.PROCESSING);
        document = sessionDocumentRepository.save(document);

        indexDocumentAsync(document.getId());

        return toResponse(document);
    }

    @Async
    @Transactional
    public void indexDocumentAsync(UUID documentId) {
        SessionDocument document =
                sessionDocumentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("SessionDocument with id {} not found for indexing", documentId);
            return;
        }

        try {
            document.setIndexingStatus(SessionDocumentStatus.PROCESSING);
            document.setErrorMessage(null);
            sessionDocumentRepository.save(document);

            Path path = Paths.get(document.getStoragePath());
            String text;
            try (InputStream in = Files.newInputStream(path)) {
                text = tika.parseToString(in);
            }

            if (text == null || text.isBlank()) {
                document.setIndexingStatus(SessionDocumentStatus.READY);
                sessionDocumentRepository.save(document);
                return;
            }

            List<Document> chunks = chunkText(text, document);
            if (!chunks.isEmpty()) {
                vectorStore.add(chunks);
            }

            document.setIndexingStatus(SessionDocumentStatus.READY);
            document.setErrorMessage(null);
            sessionDocumentRepository.save(document);
        } catch (Exception e) {
            log.error("Failed to index document {}", documentId, e);
            document.setIndexingStatus(SessionDocumentStatus.FAILED);
            String message = e.getMessage();
            if (message != null && message.length() > 500) {
                message = message.substring(0, 500);
            }
            document.setErrorMessage(message);
            sessionDocumentRepository.save(document);
        }
    }

    @Transactional(readOnly = true)
    public List<SessionDocumentResponse> getDocuments(UUID userId, UUID sessionId) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        List<SessionDocument> docs = sessionDocumentRepository.findBySessionOrderByCreatedAtDesc(session);
        List<SessionDocumentResponse> result = new ArrayList<>();
        for (SessionDocument doc : docs) {
            result.add(toResponse(doc));
        }
        return result;
    }

    @Transactional
    public void deleteDocument(UUID userId, UUID sessionId, UUID documentId) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        SessionDocument document = sessionDocumentRepository
                .findByIdAndSession(documentId, session)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        try {
            Path path = Paths.get(document.getStoragePath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete file for document {}", documentId, e);
        }

        sessionDocumentRepository.delete(document);
    }

    private SessionDocumentResponse toResponse(SessionDocument document) {
        return new SessionDocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getIndexingStatus(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private List<Document> chunkText(String text, SessionDocument document) {
        List<Document> result = new ArrayList<>();

        int chunkSize = 1000;
        int overlap = 200;
        int length = text.length();
        int index = 0;
        int chunkIndex = 0;

        while (index < length) {
            int end = Math.min(length, index + chunkSize);
            String chunk = text.substring(index, end).trim();
            if (!chunk.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("sessionId", document.getSession().getId().toString());
                metadata.put("documentId", document.getId().toString());
                metadata.put("filename", document.getOriginalFilename());
                metadata.put("chunkIndex", chunkIndex);
                metadata.put("source", "session-documents");
                result.add(new Document(chunk, metadata));
                chunkIndex++;
            }
            if (end == length) {
                break;
            }
            index = end - overlap;
            if (index < 0 || index >= length) {
                break;
            }
        }

        return result;
    }
}
