package com.xiang.xiangaicodemother.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.io.FileUtil;
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
import java.nio.file.StandardOpenOption;

/** AI 写入 Vue 工程文件的受限工具。 */
@Component
@Slf4j
public class FileWriteTool extends BaseTool {

    public FileWriteTool() {
        super();
    }

    FileWriteTool(Path outputRoot) {
        super(outputRoot);
    }

    @Tool("将一个文本文件写入当前 Vue 工程；路径必须是项目内的相对路径")
    public String writeFile(@P("文件相对路径，例如 src/App.vue") String relativeFilePath,
                            @P("完整文件内容") String content,
                            @ToolMemoryId Long appId) {
        try {
            validateContent(content, "文件内容");
            Path projectRoot = resolveProjectRoot(appId, true);
            Path target = resolveProjectPath(relativeFilePath, appId, false, true);
            createSafeParentDirectories(target, projectRoot);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(target)) {
                throw new IllegalArgumentException("目标文件不能是符号链接");
            }
            Files.writeString(target, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            log.info("Vue 工程文件写入成功，appId={}，path={}", appId, relativeFilePath);
            return "文件写入成功: " + displayPath(relativeFilePath);
        } catch (Exception e) {
            log.warn("Vue 工程文件写入被拒绝，appId={}，path={}，reason={}",
                    appId, relativeFilePath, e.getMessage());
            return failure("文件写入", e);
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativePath = arguments.getStr("relativeFilePath");
        String suffix = StrUtil.blankToDefault(FileUtil.getSuffix(relativePath), "text");
        String content = displayContent(arguments.getStr("content"));
        return String.format("[工具调用] %s %s\n````%s\n%s\n````",
                getDisplayName(), displayPath(relativePath), suffix, content);
    }
}
