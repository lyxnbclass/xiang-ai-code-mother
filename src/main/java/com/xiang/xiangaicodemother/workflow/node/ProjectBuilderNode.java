package com.xiang.xiangaicodemother.workflow.node;

import com.xiang.xiangaicodemother.core.builder.VueProjectBuilder;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/** 构建 Vue 工程并验证 dist 输出。 */
@Component
@RequiredArgsConstructor
public class ProjectBuilderNode {
    private final VueProjectBuilder vueProjectBuilder;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.from(state);
            if (!vueProjectBuilder.buildProject(context.getGeneratedCodeDir())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Vue 项目构建失败");
            }
            context.setBuildResultDir(Path.of(context.getGeneratedCodeDir(), "dist").toString());
            context.setCurrentStep("项目构建");
            return WorkflowContext.save(context);
        });
    }
}
