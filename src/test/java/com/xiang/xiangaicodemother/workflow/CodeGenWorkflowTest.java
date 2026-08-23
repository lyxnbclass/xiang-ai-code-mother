package com.xiang.xiangaicodemother.workflow;

import com.xiang.xiangaicodemother.config.properties.WorkflowProperties;
import com.xiang.xiangaicodemother.model.enums.CodeGenTypeEnum;
import com.xiang.xiangaicodemother.workflow.model.QualityResult;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeGenWorkflowTest {

    private final CodeGenWorkflow workflow = new CodeGenWorkflow(
            null, null, null, null, null, null, new WorkflowProperties());

    @Test
    void routesVueToBuildAfterQualityPasses() {
        WorkflowContext context = WorkflowContext.builder()
                .generationType(CodeGenTypeEnum.VUE_PROJECT)
                .qualityResult(QualityResult.builder().valid(true).build())
                .build();
        assertEquals("build", workflow.routeAfterQualityCheck(
                new MessagesState<>(WorkflowContext.save(context))));
    }

    @Test
    void retriesOnlyWithinConfiguredLimit() {
        WorkflowContext context = WorkflowContext.builder()
                .generationType(CodeGenTypeEnum.HTML)
                .qualityResult(QualityResult.builder().valid(false).errors(List.of("syntax")).build())
                .qualityRetryCount(1)
                .maxQualityRetries(1)
                .build();
        assertEquals("retry", workflow.routeAfterQualityCheck(
                new MessagesState<>(WorkflowContext.save(context))));
        context.setQualityRetryCount(2);
        assertEquals("abort", workflow.routeAfterQualityCheck(
                new MessagesState<>(WorkflowContext.save(context))));
    }
}
