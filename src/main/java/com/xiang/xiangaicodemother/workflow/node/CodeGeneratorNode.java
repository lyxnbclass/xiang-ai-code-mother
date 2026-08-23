package com.xiang.xiangaicodemother.workflow.node;

import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.core.AiCodeGeneratorFacade;
import com.xiang.xiangaicodemother.workflow.model.QualityResult;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/** 调用现有代码生成门面，并在质检失败后进行增量修复。 */
@Component
@RequiredArgsConstructor
public class CodeGeneratorNode {
    private final AiCodeGeneratorFacade codeGeneratorFacade;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.from(state);
            String prompt = buildPrompt(context);
            codeGeneratorFacade.generateAndSaveCodeStream(
                            prompt, context.getGenerationType(), context.getAppId())
                    .blockLast(Duration.ofMinutes(10));
            Path generated = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR,
                    context.getGenerationType().getValue() + "_" + context.getAppId());
            context.setGeneratedCodeDir(generated.toAbsolutePath().normalize().toString());
            context.setCurrentStep(context.getQualityRetryCount() == 0 ? "代码生成" : "代码修复");
            return WorkflowContext.save(context);
        });
    }

    static String buildPrompt(WorkflowContext context) {
        QualityResult result = context.getQualityResult();
        if (result == null || result.passed()) {
            return context.getEnhancedPrompt();
        }
        StringBuilder prompt = new StringBuilder(context.getEnhancedPrompt())
                .append("\n\n## 代码修复要求\n请修改现有项目并修复以下严重问题：\n");
        if (result.getErrors() != null) {
            result.getErrors().forEach(error -> prompt.append("- ").append(error).append('\n'));
        }
        if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
            prompt.append("建议：\n");
            result.getSuggestions().forEach(item -> prompt.append("- ").append(item).append('\n'));
        }
        prompt.append("保持原始需求和已有正确功能不变。原生模式必须返回完整文件，Vue 工程模式只做必要的增量修改。");
        return prompt.toString();
    }
}
