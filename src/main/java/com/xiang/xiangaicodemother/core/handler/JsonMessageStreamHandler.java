package com.xiang.xiangaicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiang.xiangaicodemother.ai.model.message.AiResponseMessage;
import com.xiang.xiangaicodemother.ai.model.message.StreamMessage;
import com.xiang.xiangaicodemother.ai.model.message.StreamMessageTypeEnum;
import com.xiang.xiangaicodemother.ai.model.message.ToolExecutedMessage;
import com.xiang.xiangaicodemother.ai.model.message.ToolRequestMessage;
import com.xiang.xiangaicodemother.ai.tools.BaseTool;
import com.xiang.xiangaicodemother.ai.tools.ToolManager;
import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.core.builder.VueProjectBuilder;
import com.xiang.xiangaicodemother.model.entity.User;
import com.xiang.xiangaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.xiang.xiangaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class JsonMessageStreamHandler {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ToolManager toolManager;

    public JsonMessageStreamHandler() {
    }

    JsonMessageStreamHandler(ToolManager toolManager) {
        this.toolManager = toolManager;
    }

    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        StringBuilder history = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> handleChunk(chunk, history, seenToolIds))
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    if (StrUtil.isNotBlank(history)) {
                        chatHistoryService.addChatMessage(appId, history.toString(),
                                ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    }
                    Path projectPath = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + appId);
                    vueProjectBuilder.buildProjectAsync(projectPath.toString());
                })
                .doOnError(error -> chatHistoryService.addChatMessage(appId,
                        "AI 回复失败：" + StrUtil.blankToDefault(error.getMessage(), "未知错误"),
                        ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId()));
    }

    String handleChunk(String chunk, StringBuilder history, Set<String> seenToolIds) {
        try {
            StreamMessage message = JSONUtil.toBean(chunk, StreamMessage.class);
            StreamMessageTypeEnum type = StreamMessageTypeEnum.getEnumByValue(message.getType());
            if (type == null) {
                log.warn("忽略未知流消息类型: {}", message.getType());
                return "";
            }
            return switch (type) {
                case AI_RESPONSE -> handleAiResponse(chunk, history);
                case TOOL_REQUEST -> handleToolRequest(chunk, seenToolIds);
                case TOOL_EXECUTED -> handleToolExecuted(chunk, history);
            };
        } catch (Exception e) {
            log.warn("忽略无法解析的 Vue 流消息", e);
            return "";
        }
    }

    private String handleAiResponse(String chunk, StringBuilder history) {
        String data = JSONUtil.toBean(chunk, AiResponseMessage.class).getData();
        if (data != null) {
            history.append(data);
            return data;
        }
        return "";
    }

    private String handleToolRequest(String chunk, Set<String> seenToolIds) {
        ToolRequestMessage request = JSONUtil.toBean(chunk, ToolRequestMessage.class);
        String id = request.getId();
        if (id != null && seenToolIds.add(id)) {
            BaseTool tool = toolManager.getTool(request.getName());
            if (tool == null) {
                log.warn("AI 请求了未注册工具: {}", request.getName());
                return String.format("\n\n[选择工具] 未知工具 %s\n\n",
                        StrUtil.blankToDefault(request.getName(), "unknown"));
            }
            return tool.generateToolRequestResponse();
        }
        return "";
    }

    private String handleToolExecuted(String chunk, StringBuilder history) {
        ToolExecutedMessage executed = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
        BaseTool tool = toolManager.getTool(executed.getName());
        if (tool == null) {
            log.warn("AI 执行了未注册工具: {}", executed.getName());
            return "";
        }
        JSONObject arguments = JSONUtil.parseObj(
                StrUtil.blankToDefault(executed.getArguments(), "{}"));
        String result = tool.generateToolExecutedResult(arguments);
        if (StrUtil.isBlank(result)) {
            return "";
        }
        String output = String.format("\n\n%s\n\n", result);
        history.append(output);
        return output;
    }
}
