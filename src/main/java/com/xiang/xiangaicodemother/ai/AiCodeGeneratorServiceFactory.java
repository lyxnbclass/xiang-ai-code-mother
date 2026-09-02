package com.xiang.xiangaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xiang.xiangaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.xiang.xiangaicodemother.ai.tools.ToolManager;
import com.xiang.xiangaicodemother.config.properties.AiChatMemoryProperties;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import com.xiang.xiangaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;

import java.time.Duration;

/**
 * 按应用创建并缓存带独立对话记忆的 AI 服务。
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    @Resource
    private AiChatMemoryProperties chatMemoryProperties;

    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) ->
                    log.debug("AI 服务实例被移除，appId={}，原因={}", key, cause))
            .build();

    private final Cache<Long, VueCodeGeneratorService> vueServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        if (appId <= 0) {
            throw new IllegalArgumentException("appId 必须大于 0");
        }
        if (codeGenType == null) {
            throw new IllegalArgumentException("代码生成类型不能为空");
        }
        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            throw new IllegalArgumentException("Vue 工程请使用专用 AI 服务");
        }
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    public VueCodeGeneratorService getVueCodeGeneratorService(long appId) {
        if (appId <= 0) {
            throw new IllegalArgumentException("appId 必须大于 0");
        }
        return vueServiceCache.get(appId, this::createVueCodeGeneratorService);
    }

    /**
     * 数据库历史发生删除时，同时清理本地服务缓存和 Redis 记忆。
     */
    public void clearAppChatMemory(long appId) {
        if (appId <= 0) {
            return;
        }
        String cacheKeyPrefix = appId + "_";
        serviceCache.asMap().keySet().removeIf(key -> key.startsWith(cacheKeyPrefix));
        vueServiceCache.invalidate(appId);
        try {
            redisChatMemoryStore.deleteMessages(appId);
        } catch (Exception e) {
            log.error("清理应用对话记忆失败，appId={}", appId, e);
        }
    }

    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId={}、类型={} 创建 AI 服务实例", appId, codeGenType.getValue());
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(chatMemoryProperties.getMaxMessages())
                .build();
        chatHistoryService.loadChatHistoryToMemory(
                appId, chatMemory, chatMemoryProperties.getMaxMessages());
        return switch (codeGenType) {
            case HTML, MULTI_FILE -> {
                StreamingChatModel streamingChatModel = applicationContext.getBean(
                        "streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(streamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    private VueCodeGeneratorService createVueCodeGeneratorService(long appId) {
        log.info("为 appId={} 创建 Vue 工程 AI 服务实例", appId);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(chatMemoryProperties.getMaxMessages())
                .build();
        chatHistoryService.loadChatHistoryToMemory(
                appId, chatMemory, chatMemoryProperties.getMaxMessages());
        StreamingChatModel reasoningStreamingChatModel = applicationContext.getBean(
                "reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(VueCodeGeneratorService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools((Object[]) toolManager.getAllTools())
                .hallucinatedToolNameStrategy(request -> ToolExecutionResultMessage.from(
                        request, "Error: there is no tool called " + request.name()))
                .maxSequentialToolsInvocations(20)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    /**
     * 保留默认 Bean，兼容已有注入点和测试；应用对话使用按 appId 创建的实例。
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        StreamingChatModel streamingChatModel = applicationContext.getBean(
                "streamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
