package com.xiang.xiangaicodemother.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 对话记忆窗口配置。
 */
@Component
@ConfigurationProperties(prefix = "ai.chat-memory")
public class AiChatMemoryProperties {

    private int maxMessages = 50;

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("AI 对话记忆窗口必须大于 0");
        }
        this.maxMessages = maxMessages;
    }
}
