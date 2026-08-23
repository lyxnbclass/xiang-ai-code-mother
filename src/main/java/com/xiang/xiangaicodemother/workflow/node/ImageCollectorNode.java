package com.xiang.xiangaicodemother.workflow.node;

import com.xiang.xiangaicodemother.workflow.ai.ImageCollectionPlanService;
import com.xiang.xiangaicodemother.workflow.model.ImageCollectionPlan;
import com.xiang.xiangaicodemother.workflow.model.ImageResource;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import com.xiang.xiangaicodemother.workflow.tool.ContentImageSearchTool;
import com.xiang.xiangaicodemother.workflow.tool.IllustrationSearchTool;
import com.xiang.xiangaicodemother.workflow.tool.LogoGeneratorTool;
import com.xiang.xiangaicodemother.workflow.tool.MermaidDiagramTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/** 先由 AI 规划，再使用虚拟线程并发收集图片素材。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCollectorNode {
    private static final int MAX_TASKS_PER_TYPE = 2;

    private final ImageCollectionPlanService planService;
    private final ContentImageSearchTool contentImageSearchTool;
    private final IllustrationSearchTool illustrationSearchTool;
    private final MermaidDiagramTool mermaidDiagramTool;
    private final LogoGeneratorTool logoGeneratorTool;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.from(state);
            List<ImageResource> images = new ArrayList<>();
            if (context.isExistingProject()) {
                context.setImageList(images);
                context.setCurrentStep("复用已有图片素材");
                return WorkflowContext.save(context);
            }
            try {
                ImageCollectionPlan plan = planService.plan(context.getOriginalPrompt());
                context.setImageCollectionPlan(plan);
                if (plan != null) {
                    images.addAll(collect(plan));
                }
            } catch (Exception e) {
                log.warn("图片规划或收集失败，跳过素材增强: {}", e.getMessage());
            }
            context.setImageList(images);
            context.setCurrentStep("图片素材收集");
            return WorkflowContext.save(context);
        });
    }

    List<ImageResource> collect(ImageCollectionPlan plan) {
        List<Callable<List<ImageResource>>> tasks = new ArrayList<>();
        limited(plan.getContentImageTasks()).forEach(task ->
                tasks.add(() -> contentImageSearchTool.search(task.query())));
        limited(plan.getIllustrationTasks()).forEach(task ->
                tasks.add(() -> illustrationSearchTool.search(task.query())));
        limited(plan.getDiagramTasks()).forEach(task ->
                tasks.add(() -> mermaidDiagramTool.generate(task.mermaidCode(), task.description())));
        limited(plan.getLogoTasks()).forEach(task ->
                tasks.add(() -> logoGeneratorTool.generate(task.description())));
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<ImageResource> result = new ArrayList<>();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<ImageResource>>> futures = executor.invokeAll(tasks);
            for (Future<List<ImageResource>> future : futures) {
                try {
                    List<ImageResource> resources = future.get();
                    if (resources != null) {
                        result.addAll(resources);
                    }
                } catch (Exception e) {
                    log.warn("单个图片任务失败: {}", e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LinkedHashMap<String, ImageResource> unique = new LinkedHashMap<>();
        result.stream().filter(resource -> resource != null && resource.getUrl() != null)
                .forEach(resource -> unique.putIfAbsent(resource.getUrl(), resource));
        return List.copyOf(unique.values());
    }

    private static <T> List<T> limited(List<T> tasks) {
        return tasks == null ? List.of() : tasks.stream().limit(MAX_TASKS_PER_TYPE).toList();
    }
}
