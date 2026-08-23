package com.xiang.xiangaicodemother.workflow;

import com.xiang.xiangaicodemother.config.properties.WorkflowProperties;
import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import com.xiang.xiangaicodemother.workflow.node.CodeGeneratorNode;
import com.xiang.xiangaicodemother.workflow.node.CodeQualityCheckNode;
import com.xiang.xiangaicodemother.workflow.node.ImageCollectorNode;
import com.xiang.xiangaicodemother.workflow.node.ProjectBuilderNode;
import com.xiang.xiangaicodemother.workflow.node.PromptEnhancerNode;
import com.xiang.xiangaicodemother.workflow.node.RouterNode;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/** 图片增强、代码生成、质量检查和项目构建工作流。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenWorkflow {
    private final ImageCollectorNode imageCollectorNode;
    private final PromptEnhancerNode promptEnhancerNode;
    private final RouterNode routerNode;
    private final CodeGeneratorNode codeGeneratorNode;
    private final CodeQualityCheckNode codeQualityCheckNode;
    private final ProjectBuilderNode projectBuilderNode;
    private final WorkflowProperties properties;

    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("image_collector", imageCollectorNode.action())
                    .addNode("prompt_enhancer", promptEnhancerNode.action())
                    .addNode("router", routerNode.action())
                    .addNode("code_generator", codeGeneratorNode.action())
                    .addNode("quality_check", codeQualityCheckNode.action())
                    .addNode("project_builder", projectBuilderNode.action())
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "quality_check")
                    .addConditionalEdges("quality_check", edge_async(this::routeAfterQualityCheck), Map.of(
                            "retry", "code_generator",
                            "build", "project_builder",
                            "finish", END,
                            "abort", END))
                    .addEdge("project_builder", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "代码生成工作流创建失败");
        }
    }

    /** 以适合现有聊天 SSE 的 Markdown 文本流执行工作流。 */
    public Flux<String> executeWithFlux(String prompt, Long appId, CodeGenTypeEnum generationType) {
        return Flux.create(sink -> Thread.startVirtualThread(() -> {
            WorkflowContext context = WorkflowContext.builder()
                    .appId(appId)
                    .originalPrompt(prompt)
                    .enhancedPrompt(prompt)
                    .generationType(generationType)
                    .currentStep("初始化")
                    .maxQualityRetries(Math.max(0, properties.getMaxQualityRetries()))
                    .existingProject(hasGeneratedProject(appId, generationType))
                    .build();
            try {
                CompiledGraph<MessagesState<String>> graph = createWorkflow();
                GraphRepresentation representation = graph.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("代码生成工作流图:\n{}", representation.content());
                sink.next("🧭 AI 工作流已启动，正在分析需求并准备素材。\n\n");
                for (NodeOutput<MessagesState<String>> step : graph.stream(Map.of(WorkflowContext.STATE_KEY, context))) {
                    if (sink.isCancelled()) {
                        return;
                    }
                    WorkflowContext current = WorkflowContext.from(step.state());
                    sink.next(formatProgress(current));
                    context = current;
                }
                if (context.getQualityResult() != null && !context.getQualityResult().passed()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,
                            "代码质量检查仍未通过，请调整需求后重试");
                }
                sink.next("🎉 工作流执行完成，网站代码已生成并通过检查。\n");
                sink.complete();
            } catch (Exception e) {
                log.error("代码生成工作流执行失败，appId={}", appId, e);
                sink.error(e);
            }
        }));
    }

    String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.from(state);
        if (context.getQualityResult() == null || !context.getQualityResult().passed()) {
            return context.getQualityRetryCount() <= context.getMaxQualityRetries() ? "retry" : "abort";
        }
        return context.getGenerationType() == CodeGenTypeEnum.VUE_PROJECT ? "build" : "finish";
    }

    private String formatProgress(WorkflowContext context) {
        String step = context.getCurrentStep();
        if ("图片素材收集".equals(step)) {
            int count = context.getImageList() == null ? 0 : context.getImageList().size();
            return "✅ 图片素材收集完成（" + count + " 个可用资源）。\n\n";
        }
        if ("复用已有图片素材".equals(step)) {
            return "✅ 已识别为增量修改，复用现有素材并跳过重复搜图。\n\n";
        }
        if ("代码质量检查未通过".equals(step)) {
            return "🔧 代码质量检查发现问题，正在进行第 " + context.getQualityRetryCount() + " 次修复。\n\n";
        }
        return "✅ " + step + "完成。\n\n";
    }

    private boolean hasGeneratedProject(Long appId, CodeGenTypeEnum generationType) {
        if (appId == null || appId <= 0 || generationType == null) {
            return false;
        }
        Path path = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR,
                generationType.getValue() + "_" + appId).toAbsolutePath().normalize();
        return Files.isDirectory(path);
    }
}
