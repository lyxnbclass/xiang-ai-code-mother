package com.xiang.xiangaicodemother.config;

import com.xiang.xiangaicodemother.monitor.AiModelMonitorListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;

/**
 * Vue 工程生成专用流式模型。独立 Bean 避免和 starter 自动配置的模型重名。
 */
@Configuration
public class ReasoningStreamingChatModelConfig {

    @Bean("reasoningStreamingChatModelPrototype")
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModelPrototype(
            @Value("${langchain4j.open-ai.streaming-chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.streaming-chat-model.api-key}") String apiKey,
            @Value("${VUE_REASONING_MODEL_NAME:deepseek-chat}") String modelName,
            @Value("${VUE_REASONING_MAX_TOKENS:8192}") Integer maxTokens,
            @Value("${VUE_REASONING_TEMPERATURE:0.1}") Double temperature,
            AiModelMonitorListener aiModelMonitorListener) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(false)
                .logResponses(false)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }
}
