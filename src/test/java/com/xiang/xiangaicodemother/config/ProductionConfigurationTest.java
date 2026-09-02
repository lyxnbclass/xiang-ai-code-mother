package com.xiang.xiangaicodemother.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductionConfigurationTest {

    @Test
    void productionExampleShouldBeValidYaml() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(
                "production-example",
                new ClassPathResource("application-prod.example.yml"));

        assertFalse(propertySources.isEmpty());
        PropertySource<?> source = propertySources.getFirst();
        assertEquals("${APP_DEPLOY_HOST:http://localhost/dist}",
                source.getProperty("code.deploy-host"));
        assertEquals("${AI_CHAT_MODEL_NAME:glm-4.6}",
                source.getProperty("langchain4j.open-ai.chat-model.model-name"));
        assertEquals("${AI_MODEL_TIMEOUT:300s}",
                source.getProperty("langchain4j.open-ai.streaming-chat-model.timeout"));
        assertEquals("${AI_CHAT_MEMORY_MAX_MESSAGES:50}",
                source.getProperty("ai.chat-memory.max-messages"));
    }
}
