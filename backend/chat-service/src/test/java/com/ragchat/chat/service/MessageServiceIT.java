package com.ragchat.chat.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ragchat.chat.config.ChatServicePostgresTestConfig;
import com.ragchat.chat.model.dto.request.CreateMessageRequest;
import com.ragchat.chat.model.dto.response.MessageResponse;
import com.ragchat.chat.model.entity.ChatMessage;
import com.ragchat.chat.model.entity.ChatSession;
import com.ragchat.chat.model.enums.MessageSender;
import com.ragchat.chat.repository.ChatMessageRepository;
import com.ragchat.chat.repository.ChatSessionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@Import(ChatServicePostgresTestConfig.class)
@ExtendWith(SpringExtension.class)
class MessageServiceIT {

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        ChatServicePostgresTestConfig.registerProperties(registry);
    }

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void createMessage_persistsAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName("IT Session")
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);

        CreateMessageRequest request = new CreateMessageRequest(MessageSender.USER, "Hello from IT", null);

        MessageResponse response = messageService.createMessage(userId, session.getId(), request);

        assertNotNull(response.id());
        assertEquals(MessageSender.USER, response.sender());
        assertEquals("Hello from IT", response.content());
        assertEquals(1, response.messageOrder());

        List<ChatMessage> stored = chatMessageRepository.findBySessionOrderByMessageOrderAsc(session);
        assertEquals(1, stored.size());
        assertEquals("Hello from IT", stored.get(0).getContent());
    }

    @Test
    void getMessages_returnsMessagesInOrder() {
        UUID userId = UUID.randomUUID();
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName("Ordered Session")
                .favorite(false)
                .build();

        session = chatSessionRepository.save(session);

        messageService.createMessage(
                userId, session.getId(), new CreateMessageRequest(MessageSender.USER, "First", null));
        messageService.createMessage(
                userId, session.getId(), new CreateMessageRequest(MessageSender.USER, "Second", null));

        List<MessageResponse> responses = messageService.getMessages(userId, session.getId());

        assertEquals(2, responses.size());
        assertEquals("First", responses.get(0).content());
        assertEquals(1, responses.get(0).messageOrder());
        assertEquals("Second", responses.get(1).content());
        assertEquals(2, responses.get(1).messageOrder());
    }
}
