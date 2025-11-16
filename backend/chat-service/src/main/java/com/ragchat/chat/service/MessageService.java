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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

    @Transactional
    public void generateResponse(ChatMessage message) {
        ChatMessage responseMessage;
        try {
            String response =
                    chatClient.prompt().user(message.getContent()).call().content();
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

        final ChatMessage sendToAi = message;

        CompletableFuture.runAsync(() -> generateResponse(sendToAi));
        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID userId, UUID sessionId) {
        ChatSession session = chatSessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        return chatMessageRepository.findBySessionOrderByMessageOrderAsc(session).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
