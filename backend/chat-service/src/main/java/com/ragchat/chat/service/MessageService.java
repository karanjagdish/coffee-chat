package com.ragchat.chat.service;

import com.ragchat.chat.config.ChatHistoryProperties;
import com.ragchat.chat.exception.ResourceNotFoundException;
import com.ragchat.chat.model.dto.context.ChatMessageContext;
import com.ragchat.chat.model.dto.context.ContextDocument;
import com.ragchat.chat.model.dto.request.CreateMessageRequest;
import com.ragchat.chat.model.dto.response.MessageResponse;
import com.ragchat.chat.model.dto.response.PageResponse;
import com.ragchat.chat.model.entity.ChatMessage;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.model.enums.MessageSender;
import com.ragchat.chat.repository.ChatMessageRepository;
import com.ragchat.chat.repository.ChatSessionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ChatHistoryProperties chatHistoryProperties;

    @Transactional
    public void generateResponse(ChatMessage message) {
        ChatMessage responseMessage;
        try {
            String userContent = message.getContent();
            List<Document> contextDocuments = retrieveContextDocuments(message, userContent);
            List<ChatMessage> recentMessages = loadRecentMessages(message, chatHistoryProperties.getPreviousMessages());
            String prompt = buildPrompt(userContent, contextDocuments, recentMessages);

            String response = chatClient.prompt().user(prompt).call().content();
            log.info("Received AI response: {}", response);

            ChatMessageContext context = buildContextPayload(contextDocuments);
            responseMessage = ChatMessage.builder()
                    .session(message.getSession())
                    .sender(MessageSender.AI)
                    .content(response)
                    .context(context)
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

        indexChatMessage(responseMessage);
    }

    private List<Document> retrieveContextDocuments(ChatMessage message, String userContent) {
        List<Document> contextDocuments = Collections.emptyList();
        try {
            String filter = "sessionId == '" + message.getSession().getId() + "' && source == 'session-documents'";
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(userContent)
                    .topK(5)
                    .filterExpression(filter)
                    .build();

            contextDocuments = vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.warn(
                    "Vector search failed for session {}: {}",
                    message.getSession().getId(),
                    e.getMessage());
        }
        return contextDocuments;
    }

    private String buildPrompt(String userContent, List<Document> contextDocuments, List<ChatMessage> recentMessages) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append(
                "You are a helpful assistant. Use any provided context and recent conversation to answer the user's question. If the context is not relevant, you may also use your general knowledge, but prefer the provided context when possible.\n");

        if (contextDocuments != null && !contextDocuments.isEmpty()) {
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
                promptBuilder
                        .append("[")
                        .append(index)
                        .append("] ")
                        .append(snippet)
                        .append("\n\n");
                used += snippet.length();
                index++;
            }
        }

        if (recentMessages != null && !recentMessages.isEmpty()) {
            promptBuilder.append(
                    "Recent conversation (lines starting with 'User:' are the human, 'Assistant:' are you):\n");
            for (ChatMessage m : recentMessages) {
                String role = m.getSender() == MessageSender.USER ? "User" : "Assistant";
                promptBuilder.append(role).append(": ").append(m.getContent()).append("\n");
            }
            promptBuilder.append("\n");
        }

        promptBuilder.append("User question:\n");
        promptBuilder.append(userContent);
        promptBuilder.append("\n\n");
        promptBuilder.append(
                "When you answer, respond only with the answer text itself. Do not include any speaker labels like 'Assistant:' or 'User:' in your response.\n\n");


        return promptBuilder.toString();
    }

    private List<ChatMessage> loadRecentMessages(ChatMessage message, int limit) {
        List<ChatMessage> allMessages =
                chatMessageRepository.findBySessionOrderByMessageOrderAsc(message.getSession());
        if (allMessages.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        List<ChatMessage> priorMessages = new ArrayList<>();
        for (ChatMessage m : allMessages) {
            if (m.getMessageOrder() != null
                    && message.getMessageOrder() != null
                    && m.getMessageOrder() < message.getMessageOrder()) {
                priorMessages.add(m);
            }
        }

        if (priorMessages.isEmpty()) {
            return Collections.emptyList();
        }

        int userCount = 0;
        int assistantCount = 0;
        List<ChatMessage> selected = new ArrayList<>();

        for (int i = priorMessages.size() - 1; i >= 0; i--) {
            ChatMessage m = priorMessages.get(i);
            if (m.getSender() == MessageSender.USER) {
                if (userCount >= limit) {
                    continue;
                }
                selected.add(m);
                userCount++;
            } else if (m.getSender() == MessageSender.AI) {
                if (assistantCount >= limit) {
                    continue;
                }
                selected.add(m);
                assistantCount++;
            }

            if (userCount >= limit && assistantCount >= limit) {
                break;
            }
        }

        Collections.reverse(selected);
        return selected;
    }

    private ChatMessageContext buildContextPayload(List<Document> contextDocuments) {
        if (contextDocuments == null || contextDocuments.isEmpty()) {
            return null;
        }

        List<ContextDocument> documents = new ArrayList<>();
        for (Document doc : contextDocuments) {
            Map<String, Object> metadata = doc.getMetadata();

            String sessionId = metadata != null ? (String) metadata.get("sessionId") : null;
            String documentId = metadata != null ? (String) metadata.get("documentId") : null;
            String filename = metadata != null ? (String) metadata.get("filename") : null;

            Integer chunkIndex = null;
            if (metadata != null && metadata.get("chunkIndex") != null) {
                Object idx = metadata.get("chunkIndex");
                if (idx instanceof Number number) {
                    chunkIndex = number.intValue();
                } else if (idx instanceof String s) {
                    try {
                        chunkIndex = Integer.parseInt(s);
                    } catch (NumberFormatException ignored) {
                        // ignore invalid chunk index
                    }
                }
            }

            String snippet = doc.getText();
            if (snippet != null) {
                int maxSnippetLength = 500;
                if (snippet.length() > maxSnippetLength) {
                    snippet = snippet.substring(0, maxSnippetLength);
                }
            }

            Double score = doc.getScore();

            ContextDocument contextDocument = ContextDocument.builder()
                    .sessionId(sessionId)
                    .documentId(documentId)
                    .filename(filename)
                    .chunkIndex(chunkIndex)
                    .snippet(snippet)
                    .score(score)
                    .build();

            documents.add(contextDocument);
        }

        return ChatMessageContext.builder()
                .source("session-documents")
                .documents(documents)
                .build();
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
                .context(null)
                .messageOrder(nextOrder)
                .build();
        message = chatMessageRepository.save(message);

        log.debug("Created message with id: {}", message.getId());

        indexChatMessage(message);

        generateResponse(message);
        return toResponse(message);
    }

    private void indexChatMessage(ChatMessage message) {
        try {
            if (message.getContent() == null || message.getContent().isBlank()) {
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sessionId", message.getSession().getId().toString());
            if (message.getId() != null) {
                metadata.put("messageId", message.getId().toString());
            }
            metadata.put("sender", message.getSender().name());
            metadata.put("messageOrder", message.getMessageOrder());
            metadata.put("source", "chat-message");

            Document document = new Document(message.getContent(), metadata);
            vectorStore.add(List.of(document));
            log.debug("Indexed chat message with id: {}", message.getId());
        } catch (Exception e) {
            log.warn("Failed to index chat message {} into vector store: {}", message.getId(), e.getMessage());
        }
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
                message.getMessageOrder(),
                message.getCreatedAt());
    }
}
