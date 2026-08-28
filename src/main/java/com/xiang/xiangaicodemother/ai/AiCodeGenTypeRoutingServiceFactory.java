package com.xiang.xiangaicodemother.ai;

import com.xiang.xiangaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码生成类型路由服务工厂。
 */
@Configuration(proxyBeanMethods = false)
public class AiCodeGenTypeRoutingServiceFactory {

    private final ApplicationContext applicationContext;

    public AiCodeGenTypeRoutingServiceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService() {
        ChatModel chatModel = applicationContext.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
