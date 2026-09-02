package com.xiang.xiangaicodemother.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

/** 使用低输出上限的独立模型处理生成类型分类，避免占用代码生成模型。 */
@Configuration(proxyBeanMethods = false)
public class RoutingAiModelConfig {

    @Bean("routingChatModelPrototype")
    @Scope("prototype")
    public ChatModel routingChatModelPrototype(
            @Value("${langchain4j.open-ai.routing-chat-model.base-url:${langchain4j.open-ai.chat-model.base-url}}") String baseUrl,
            @Value("${langchain4j.open-ai.routing-chat-model.api-key:${langchain4j.open-ai.chat-model.api-key}}") String apiKey,
            @Value("${langchain4j.open-ai.routing-chat-model.model-name:${langchain4j.open-ai.chat-model.model-name}}") String modelName,
            @Value("${langchain4j.open-ai.routing-chat-model.max-tokens:100}") Integer maxTokens,
            @Value("${langchain4j.open-ai.routing-chat-model.temperature:0.0}") Double temperature,
            @Value("${langchain4j.open-ai.routing-chat-model.timeout:300s}") Duration timeout,
            @Value("${AI_MODEL_MAX_RETRIES:3}") Integer maxRetries) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
