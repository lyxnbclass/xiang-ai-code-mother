package com.xiang.xiangaicodemother.config;

import com.xiang.xiangaicodemother.monitor.AiModelMonitorListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;
import java.util.List;

/** 为每个应用对话提供独立的普通流式模型，避免共享实例阻塞并发请求。 */
@Configuration(proxyBeanMethods = false)
public class StreamingChatModelConfig {

    @Bean("streamingChatModelPrototype")
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype(
            @Value("${langchain4j.open-ai.streaming-chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.streaming-chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.streaming-chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.streaming-chat-model.max-tokens:8192}") Integer maxTokens,
            @Value("${langchain4j.open-ai.streaming-chat-model.temperature:0.1}") Double temperature,
            @Value("${langchain4j.open-ai.streaming-chat-model.timeout:300s}") Duration timeout,
            AiModelMonitorListener aiModelMonitorListener) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(timeout)
                .logRequests(false)
                .logResponses(false)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }
}
