package com.xiang.xiangaicodemother.workflow.node;

import com.xiang.xiangaicodemother.workflow.ai.ImageCollectionPlanService;
import com.xiang.xiangaicodemother.workflow.model.ImageCategoryEnum;
import com.xiang.xiangaicodemother.workflow.model.ImageCollectionPlan;
import com.xiang.xiangaicodemother.workflow.model.ImageResource;
import com.xiang.xiangaicodemother.workflow.model.QualityResult;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import com.xiang.xiangaicodemother.workflow.tool.ContentImageSearchTool;
import com.xiang.xiangaicodemother.workflow.tool.IllustrationSearchTool;
import com.xiang.xiangaicodemother.workflow.tool.LogoGeneratorTool;
import com.xiang.xiangaicodemother.workflow.tool.MermaidDiagramTool;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowNodeTest {

    @Test
    void collectsEachPlannedImageTaskExactlyOnce() {
        ImageCollectionPlanService planner = mock(ImageCollectionPlanService.class);
        ContentImageSearchTool contentTool = mock(ContentImageSearchTool.class);
        IllustrationSearchTool illustrationTool = mock(IllustrationSearchTool.class);
        MermaidDiagramTool diagramTool = mock(MermaidDiagramTool.class);
        LogoGeneratorTool logoTool = mock(LogoGeneratorTool.class);
        ImageCollectionPlan plan = new ImageCollectionPlan();
        plan.setContentImageTasks(List.of(new ImageCollectionPlan.ImageSearchTask("coffee")));
        plan.setLogoTasks(List.of(new ImageCollectionPlan.LogoTask("coffee brand")));
        when(planner.plan("咖啡店官网")).thenReturn(plan);
        when(contentTool.search("coffee")).thenReturn(List.of(image(ImageCategoryEnum.CONTENT, "content")));
        when(logoTool.generate("coffee brand")).thenReturn(List.of(image(ImageCategoryEnum.LOGO, "logo")));

        WorkflowContext context = WorkflowContext.builder().originalPrompt("咖啡店官网").build();
        var node = new ImageCollectorNode(planner, contentTool, illustrationTool, diagramTool, logoTool);
        Map<String, Object> update = node.action()
                .apply(new MessagesState<>(WorkflowContext.save(context))).join();
        WorkflowContext updated = (WorkflowContext) update.get(WorkflowContext.STATE_KEY);

        assertEquals(2, updated.getImageList().size());
        verify(contentTool).search("coffee");
        verify(logoTool).generate("coffee brand");
    }

    @Test
    void enhancesPromptWithCollectedResources() {
        WorkflowContext context = WorkflowContext.builder()
                .originalPrompt("创建技术博客")
                .imageList(List.of(image(ImageCategoryEnum.ARCHITECTURE, "diagram")))
                .build();
        Map<String, Object> update = new PromptEnhancerNode().action()
                .apply(new MessagesState<>(WorkflowContext.save(context))).join();
        WorkflowContext updated = (WorkflowContext) update.get(WorkflowContext.STATE_KEY);

        assertTrue(updated.getEnhancedPrompt().contains("创建技术博客"));
        assertTrue(updated.getEnhancedPrompt().contains("https://example.com/diagram.png"));
    }

    @Test
    void skipsRepeatedImagePlanningForExistingProject() {
        ImageCollectionPlanService planner = mock(ImageCollectionPlanService.class);
        var node = new ImageCollectorNode(planner, mock(ContentImageSearchTool.class),
                mock(IllustrationSearchTool.class), mock(MermaidDiagramTool.class),
                mock(LogoGeneratorTool.class));
        WorkflowContext context = WorkflowContext.builder()
                .originalPrompt("修改标题")
                .existingProject(true)
                .build();

        node.action().apply(new MessagesState<>(WorkflowContext.save(context))).join();

        verifyNoInteractions(planner);
        assertEquals("复用已有图片素材", context.getCurrentStep());
    }

    @Test
    void repairPromptKeepsOriginalRequirements() {
        WorkflowContext context = WorkflowContext.builder()
                .enhancedPrompt("创建一个课程网站")
                .qualityResult(QualityResult.builder().valid(false)
                        .errors(List.of("App.vue 存在语法错误"))
                        .suggestions(List.of("补全结束标签"))
                        .build())
                .build();

        String prompt = CodeGeneratorNode.buildPrompt(context);

        assertTrue(prompt.contains("创建一个课程网站"));
        assertTrue(prompt.contains("App.vue 存在语法错误"));
        assertTrue(prompt.contains("补全结束标签"));
    }

    private static ImageResource image(ImageCategoryEnum category, String name) {
        return ImageResource.builder().category(category).description(name)
                .url("https://example.com/" + name + ".png").build();
    }
}
