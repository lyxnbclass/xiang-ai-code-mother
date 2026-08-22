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
import java.nio.file.StandardOpenOption;

/** AI 精确替换 Vue 工程文件片段的受限工具。 */
@Component
@Slf4j
public class FileModifyTool extends BaseTool {

    public FileModifyTool() {
        super();
    }

    FileModifyTool(Path outputRoot) {
        super(outputRoot);
    }

    @Tool("精确修改当前 Vue 工程文件：仅当旧内容在文件中唯一出现时才替换")
    public String modifyFile(@P("文件相对路径，例如 src/App.vue") String relativeFilePath,
                             @P("需要被替换的完整旧内容，必须能唯一匹配") String oldContent,
                             @P("替换后的新内容") String newContent,
                             @ToolMemoryId Long appId) {
        try {
            validateContent(oldContent, "旧内容");
            validateContent(newContent, "新内容");
            if (oldContent.isEmpty()) {
                throw new IllegalArgumentException("旧内容不能为空字符串");
            }
            Path target = resolveProjectPath(relativeFilePath, appId, false, false);
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("文件不存在或不是普通文件");
            }
            if (Files.size(target) > MAX_CONTENT_BYTES) {
                throw new IllegalArgumentException("文件超过 1 MB，不能修改");
            }
            String originalContent = Files.readString(target, StandardCharsets.UTF_8);
            int matchCount = countOccurrences(originalContent, oldContent);
            if (matchCount == 0) {
                throw new IllegalArgumentException("文件中未找到要替换的旧内容");
            }
            if (matchCount > 1) {
                throw new IllegalArgumentException("旧内容匹配到多处，请提供更完整的上下文");
            }
            String modifiedContent = originalContent.replace(oldContent, newContent);
            validateContent(modifiedContent, "修改后的文件内容");
            if (modifiedContent.equals(originalContent)) {
                return "文件内容未发生变化: " + displayPath(relativeFilePath);
            }
            Files.writeString(target, modifiedContent, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            log.info("Vue 工程文件修改成功，appId={}，path={}", appId, relativeFilePath);
            return "文件修改成功: " + displayPath(relativeFilePath);
        } catch (Exception e) {
            log.warn("Vue 工程文件修改被拒绝，appId={}，path={}，reason={}",
                    appId, relativeFilePath, e.getMessage());
            return failure("文件修改", e);
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "修改文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativePath = displayPath(arguments.getStr("relativeFilePath"));
        String oldContent = displayContent(arguments.getStr("oldContent"));
        String newContent = displayContent(arguments.getStr("newContent"));
        return String.format("[工具调用] %s %s\n\n替换前：\n````\n%s\n````"
                        + "\n\n替换后：\n````\n%s\n````",
                getDisplayName(), relativePath, oldContent, newContent);
    }

    private int countOccurrences(String source, String target) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
