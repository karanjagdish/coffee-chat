package com.ragchat.chat.service;

import com.ragchat.chat.exception.ResourceNotFoundException;
import com.ragchat.chat.model.dto.request.CreateMessageRequest;
import com.ragchat.chat.model.dto.response.MessageResponse;
import com.ragchat.chat.model.dto.response.PageResponse;
import com.ragchat.chat.model.entity.ChatMessage;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.model.enums.MessageSender;
import com.ragchat.chat.repository.ChatMessageRepository;
import com.ragchat.chat.repository.ChatSessionRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Transactional
    public void generateResponse(ChatMessage message) {
        ChatMessage responseMessage;
        try {
            String userContent = message.getContent();
            List<Document> contextDocuments = retrieveContextDocuments(message, userContent);
            String prompt = buildPrompt(userContent, contextDocuments);

            String response = chatClient.prompt().user(prompt).call().content();
            log.info("Received AI response: {}", response);
            responseMessage = ChatMessage.builder()
                    .session(message.getSession())
                    .sender(MessageSender.AI)
                    .content(response)
                    .context(null)
                    .messageOrder(message.getMessageOrder() + 1)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate response: {}", e.getMessage());
            responseMessage = ChatMessage.builder()
                    .session(message.getSession())
                    .sender(MessageSender.AI)
                    .content("Failed to generate response")
                    .context(null)
                    .messageOrder(message.getMessageOrder() + 1)
                    .build();
        }
        chatMessageRepository.save(responseMessage);
    }

    private List<Document> retrieveContextDocuments(ChatMessage message, String userContent) {
        List<Document> contextDocuments = Collections.emptyList();
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(userContent)
                    .topK(5)
                    .filterExpression("sessionId == '" + message.getSession().getId() + "'")
                    .build();

            contextDocuments = vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.warn("Vector search failed for session {}: {}", message.getSession().getId(), e.getMessage());
        }
        return contextDocuments;
    }

    private String buildPrompt(String userContent, List<Document> contextDocuments) {
        StringBuilder promptBuilder = new StringBuilder();
        if (contextDocuments != null && !contextDocuments.isEmpty()) {
            promptBuilder.append(
                    "You are a helpful assistant. Use the following context to answer the user's question. If the context is not relevant, you may also use your general knowledge, but prefer the context when possible.\n\n");
            promptBuilder.append("Context:\n");

            int maxChars = 3000;
            int used = 0;
            int index = 1;
            for (Document doc : contextDocuments) {
                String snippet = doc.getText();
                if (snippet == null || snippet.isBlank()) {
                    continue;
                }
                if (used >= maxChars) {
                    break;
                }
                if (snippet.length() + used > maxChars) {
                    snippet = snippet.substring(0, maxChars - used);
                }
                promptBuilder.append("[").append(index).append("] ").append(snippet).append("\n\n");
                used += snippet.length();
                index++;
            }

            promptBuilder.append("User question:\n");
            promptBuilder.append(userContent);
        } else {
            promptBuilder.append(userContent);
        }

        return promptBuilder.toString();
    }

    @Transactional
    public MessageResponse createMessage(UUID userId, UUID sessionId, CreateMessageRequest request) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Integer nextOrder = chatMessageRepository
                .findTopBySessionOrderByMessageOrderDesc(session)
                .map(m -> m.getMessageOrder() + 1)
                .orElse(1);

        ChatMessage message = ChatMessage.builder()
                .session(session)
                .sender(request.sender())
                .content(request.content())
                .context(request.context())
                .messageOrder(nextOrder)
                .build();
        message = chatMessageRepository.save(message);

        generateResponse(message);
        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getMessagesPage(UUID userId, UUID sessionId, int page, int size) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage = chatMessageRepository.findBySessionOrderByMessageOrderDesc(session, pageable);

        List<MessageResponse> content =
                messagePage.getContent().stream().map(this::toResponse).collect(Collectors.toList());

        return PageResponse.<MessageResponse>builder()
                .content(content)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .hasNext(messagePage.hasNext())
                .hasPrevious(messagePage.hasPrevious())
                .build();
    }

    private MessageResponse toResponse(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getSender(),
                message.getContent(),
                (message.getContext() instanceof java.util.Map)
                        ? (java.util.Map<String, Object>) message.getContext()
                        : null,
                message.getMessageOrder(),
                message.getCreatedAt());
    }
}
