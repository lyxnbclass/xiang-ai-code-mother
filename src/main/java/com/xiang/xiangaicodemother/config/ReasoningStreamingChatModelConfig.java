package com.xiang.xiangaicodemother.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vue 工程生成专用流式模型。独立 Bean 避免和 starter 自动配置的模型重名。
 */
@Configuration
public class ReasoningStreamingChatModelConfig {

    @Bean("reasoningStreamingChatModel")
    public StreamingChatModel reasoningStreamingChatModel(
            @Value("${langchain4j.open-ai.streaming-chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.streaming-chat-model.api-key}") String apiKey,
            @Value("${VUE_REASONING_MODEL_NAME:deepseek-chat}") String modelName,
            @Value("${VUE_REASONING_MAX_TOKENS:8192}") Integer maxTokens) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
