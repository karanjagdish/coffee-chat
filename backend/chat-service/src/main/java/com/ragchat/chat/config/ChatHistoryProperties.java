package com.ragchat.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chat.history")
public class ChatHistoryProperties {

    private int previousMessages = 3;

    public int getPreviousMessages() {
        return previousMessages;
    }

    public void setPreviousMessages(int previousMessages) {
        this.previousMessages = previousMessages;
    }
}
