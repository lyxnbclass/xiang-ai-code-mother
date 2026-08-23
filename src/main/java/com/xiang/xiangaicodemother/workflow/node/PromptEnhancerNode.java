package com.xiang.xiangaicodemother.workflow.node;

import cn.hutool.core.collection.CollUtil;
import com.xiang.xiangaicodemother.workflow.model.ImageResource;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/** 将已验证的图片 URL 注入代码生成提示词。 */
@Component
public class PromptEnhancerNode {

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.from(state);
            StringBuilder prompt = new StringBuilder(context.getOriginalPrompt());
            if (CollUtil.isNotEmpty(context.getImageList())) {
                prompt.append("\n\n## 可用图片素材\n请只使用下列真实 URL，并合理放置；不要虚构其他图片 URL。\n");
                for (ImageResource image : context.getImageList()) {
                    prompt.append("- ").append(image.getCategory().getText()).append("：")
                            .append(safeDescription(image.getDescription())).append(" (")
                            .append(image.getUrl()).append(")\n");
                }
            }
            context.setEnhancedPrompt(prompt.toString());
            context.setCurrentStep("提示词增强");
            return WorkflowContext.save(context);
        });
    }

    private static String safeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "图片素材";
        }
        String singleLine = description.replace('\r', ' ').replace('\n', ' ').trim();
        return singleLine.length() > 200 ? singleLine.substring(0, 200) : singleLine;
    }
}
