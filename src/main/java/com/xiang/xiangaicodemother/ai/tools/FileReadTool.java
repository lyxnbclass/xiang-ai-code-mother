package com.xiang.xiangaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** AI 读取 Vue 工程文本文件的受限工具。 */
@Component
@Slf4j
public class FileReadTool extends BaseTool {

    public FileReadTool() {
        super();
    }

    FileReadTool(Path outputRoot) {
        super(outputRoot);
    }

    @Tool("读取当前 Vue 工程内的一个文本文件；路径必须是项目内的相对路径")
    public String readFile(@P("文件相对路径，例如 src/App.vue") String relativeFilePath,
                           @ToolMemoryId Long appId) {
        try {
            Path target = resolveProjectPath(relativeFilePath, appId, false, false);
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("文件不存在或不是普通文件");
            }
            if (Files.size(target) > MAX_CONTENT_BYTES) {
                throw new IllegalArgumentException("文件超过 1 MB，不能读取");
            }
            String content = Files.readString(target, StandardCharsets.UTF_8);
            log.info("Vue 工程文件读取成功，appId={}，path={}", appId, relativeFilePath);
            return content;
        } catch (Exception e) {
            log.warn("Vue 工程文件读取被拒绝，appId={}，path={}，reason={}",
                    appId, relativeFilePath, e.getMessage());
            return failure("文件读取", e);
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("[工具调用] %s %s", getDisplayName(),
                displayPath(arguments.getStr("relativeFilePath")));
    }
}
