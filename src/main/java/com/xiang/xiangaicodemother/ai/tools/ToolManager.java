package com.xiang.xiangaicodemother.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 统一注册 AI 文件工具，并按工具方法名提供展示策略。 */
@Component
@Slf4j
public class ToolManager {

    private final BaseTool[] tools;
    private final Map<String, BaseTool> toolMap;

    public ToolManager(BaseTool[] tools) {
        this.tools = tools == null ? new BaseTool[0] : tools.clone();
        Map<String, BaseTool> registeredTools = new LinkedHashMap<>();
        for (BaseTool tool : this.tools) {
            if (tool == null) {
                continue;
            }
            BaseTool duplicate = registeredTools.putIfAbsent(tool.getToolName(), tool);
            if (duplicate != null) {
                throw new IllegalStateException("AI 工具名称重复: " + tool.getToolName());
            }
            log.info("注册 AI 工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        this.toolMap = Collections.unmodifiableMap(registeredTools);
    }

    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    public BaseTool[] getAllTools() {
        return tools.clone();
    }
}
