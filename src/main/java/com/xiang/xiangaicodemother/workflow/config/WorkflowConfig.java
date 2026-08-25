package com.xiang.xiangaicodemother.workflow.config;

import com.xiang.xiangaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.xiang.xiangaicodemother.config.properties.WorkflowProperties;
import com.xiang.xiangaicodemother.workflow.ai.CodeQualityCheckService;
import com.xiang.xiangaicodemother.workflow.ai.ImageCollectionPlanService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 工作流 AI 服务配置。 */
@Configuration
@EnableConfigurationProperties(WorkflowProperties.class)
public class WorkflowConfig {

    @Bean
    public ImageCollectionPlanService imageCollectionPlanService(
            @Qualifier("openAiChatModel") ChatModel chatModel) {
        return AiServices.builder(ImageCollectionPlanService.class)
                .chatModel(chatModel)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }

    @Bean
    public CodeQualityCheckService codeQualityCheckService(
            @Qualifier("openAiChatModel") ChatModel chatModel) {
        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(chatModel)
                .inputGuardrails(new PromptSafetyInputGuardrail())
                .build();
    }
}
