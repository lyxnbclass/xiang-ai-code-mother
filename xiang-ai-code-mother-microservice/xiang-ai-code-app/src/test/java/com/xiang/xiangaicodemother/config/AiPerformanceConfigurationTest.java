package com.xiang.xiangaicodemother.config;

import com.xiang.xiangaicodemother.config.properties.AiChatMemoryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiPerformanceConfigurationTest {

    @Test
    void applicationYamlShouldExposeStableAiDefaults() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(
                "microservice-app", new ClassPathResource("application.yml"));

        assertFalse(sources.isEmpty());
        PropertySource<?> source = sources.getFirst();
        assertEquals("${AI_MODEL_NAME:glm-4.6}",
                source.getProperty("langchain4j.open-ai.streaming-chat-model.model-name"));
        assertEquals("${AI_MODEL_TIMEOUT:300s}",
                source.getProperty("langchain4j.open-ai.streaming-chat-model.timeout"));
        assertEquals("${AI_CHAT_MEMORY_MAX_MESSAGES:50}",
                source.getProperty("ai.chat-memory.max-messages"));
    }

    @Test
    void chatMemoryShouldDefaultToFiftyAndRejectInvalidValues() {
        AiChatMemoryProperties properties = new AiChatMemoryProperties();

        assertEquals(50, properties.getMaxMessages());
        assertThrows(IllegalArgumentException.class, () -> properties.setMaxMessages(0));
    }
}
