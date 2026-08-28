package com.xiang.xiangaicodemother.workflow.node;

import com.xiang.xiangaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/** 在未指定生成类型时进行智能路由。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouterNode {
    private final AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.from(state);
            if (context.getGenerationType() == null) {
                try {
                    var routingService = routingServiceFactory.createAiCodeGenTypeRoutingService();
                    var result = routingService.routeCodeGenType(context.getOriginalPrompt());
                    context.setGenerationType(result == null ? null : result.getCodeGenType());
                    if (context.getGenerationType() == null) {
                        context.setGenerationType(CodeGenTypeEnum.HTML);
                    }
                } catch (Exception e) {
                    log.warn("工作流智能路由失败，降级为 HTML: {}", e.getMessage());
                    context.setGenerationType(CodeGenTypeEnum.HTML);
                }
            }
            context.setCurrentStep("生成类型路由");
            return WorkflowContext.save(context);
        });
    }
}
