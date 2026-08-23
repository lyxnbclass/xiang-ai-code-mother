package com.xiang.xiangaicodemother.workflow.node;

import cn.hutool.core.io.FileUtil;
import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.workflow.ai.CodeQualityCheckService;
import com.xiang.xiangaicodemother.workflow.model.QualityResult;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeQualityCheckNodeTest {

    @Test
    void readsOnlyUsefulProjectFilesAndPreservesTemplateBraces() throws Exception {
        Path root = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "quality-test-" + UUID.randomUUID());
        try {
            Files.createDirectories(root.resolve("src"));
            Files.createDirectories(root.resolve("node_modules/pkg"));
            Files.writeString(root.resolve("src/App.vue"), "<div>{{msg}}</div>");
            Files.writeString(root.resolve("node_modules/pkg/index.js"), "ignored-secret");
            Files.writeString(root.resolve("README.md"), "ignored-readme");
            String content = CodeQualityCheckNode.readProject(root.toString());

            assertTrue(content.contains("{{msg}}"));
            assertFalse(content.contains("ignored-secret"));
            assertFalse(content.contains("ignored-readme"));

            CodeQualityCheckService service = mock(CodeQualityCheckService.class);
            when(service.check(contains("{{msg}}"))).thenReturn(
                    QualityResult.builder().valid(true).errors(List.of()).suggestions(List.of()).build());
            WorkflowContext context = WorkflowContext.builder().generatedCodeDir(root.toString()).build();
            new CodeQualityCheckNode(service).action()
                    .apply(new MessagesState<>(WorkflowContext.save(context))).join();
            verify(service).check(contains("{{msg}}"));
            assertTrue(context.getQualityResult().passed());
        } finally {
            FileUtil.del(root.toFile());
        }
    }
}
