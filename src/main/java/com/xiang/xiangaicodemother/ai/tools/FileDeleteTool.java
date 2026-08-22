package com.xiang.xiangaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/** AI 删除 Vue 工程非关键文件的受限工具。 */
@Component
@Slf4j
public class FileDeleteTool extends BaseTool {

    private static final Set<String> PROTECTED_FILE_NAMES = Set.of(
            "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "vite.config.js", "vite.config.ts", "vue.config.js",
            "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
            "index.html", "main.js", "main.ts", "app.vue", ".gitignore", "readme.md"
    );

    public FileDeleteTool() {
        super();
    }

    FileDeleteTool(Path outputRoot) {
        super(outputRoot);
    }

    @Tool("删除当前 Vue 工程内的非关键普通文件；不能删除目录或工程入口文件")
    public String deleteFile(@P("文件相对路径") String relativeFilePath,
                             @ToolMemoryId Long appId) {
        try {
            Path target = resolveProjectPath(relativeFilePath, appId, false, false);
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                return "文件不存在，无需删除: " + displayPath(relativeFilePath);
            }
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("只允许删除普通文件");
            }
            String fileName = target.getFileName().toString().toLowerCase(Locale.ROOT);
            if (PROTECTED_FILE_NAMES.contains(fileName) || fileName.startsWith(".env")) {
                throw new IllegalArgumentException("不允许删除工程关键文件");
            }
            Files.delete(target);
            log.info("Vue 工程文件删除成功，appId={}，path={}", appId, relativeFilePath);
            return "文件删除成功: " + displayPath(relativeFilePath);
        } catch (Exception e) {
            log.warn("Vue 工程文件删除被拒绝，appId={}，path={}，reason={}",
                    appId, relativeFilePath, e.getMessage());
            return failure("文件删除", e);
        }
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getDisplayName() {
        return "删除文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("[工具调用] %s %s", getDisplayName(),
                displayPath(arguments.getStr("relativeFilePath")));
    }
}
