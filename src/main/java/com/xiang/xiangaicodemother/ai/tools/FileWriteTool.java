package com.xiang.xiangaicodemother.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.xiang.xiangaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** AI 写入 Vue 工程文件的受限工具。 */
@Component
@Slf4j
public class FileWriteTool {

    private static final int MAX_PATH_LENGTH = 240;
    private static final int MAX_CONTENT_BYTES = 1_048_576;

    private final Path outputRoot;

    public FileWriteTool() {
        this(Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR));
    }

    FileWriteTool(Path outputRoot) {
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
    }

    @Tool("将一个文本文件写入当前 Vue 工程；路径必须是项目内的相对路径")
    public String writeFile(@P("文件相对路径，例如 src/App.vue") String relativeFilePath,
                            @P("完整文件内容") String content,
                            @ToolMemoryId Long appId) {
        try {
            Path target = resolveSafeTarget(relativeFilePath, content, appId);
            Path parent = target.getParent();
            Path projectRoot = outputRoot.resolve("vue_project_" + appId).normalize();
            createSafeDirectories(parent, projectRoot);
            if (Files.isSymbolicLink(target)) {
                throw new IllegalArgumentException("目标文件不能是符号链接");
            }
            Files.writeString(target, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            log.info("Vue 工程文件写入成功，appId={}，path={}", appId, relativeFilePath);
            return "文件写入成功: " + Path.of(relativeFilePath).normalize().toString().replace('\\', '/');
        } catch (Exception e) {
            log.warn("Vue 工程文件写入被拒绝，appId={}，path={}，reason={}",
                    appId, relativeFilePath, e.getMessage());
            return "文件写入失败: " + StrUtil.blankToDefault(e.getMessage(), "未知错误");
        }
    }

    private Path resolveSafeTarget(String relativeFilePath, String content, Long appId) throws IOException {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("应用 ID 无效");
        }
        if (StrUtil.isBlank(relativeFilePath) || relativeFilePath.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("文件路径为空或过长");
        }
        if (content == null || content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException("文件内容为空或超过 1 MB");
        }
        Path relativePath = Path.of(relativeFilePath);
        if (relativePath.isAbsolute() || relativeFilePath.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("只允许项目内的相对路径");
        }
        relativePath = relativePath.normalize();
        if (relativePath.getNameCount() == 0 || relativePath.startsWith("..")) {
            throw new IllegalArgumentException("文件路径不能越过项目目录");
        }

        Files.createDirectories(outputRoot);
        if (Files.isSymbolicLink(outputRoot)) {
            throw new IllegalArgumentException("代码输出目录不能是符号链接");
        }
        Path projectRoot = outputRoot.resolve("vue_project_" + appId).normalize();
        if (!projectRoot.startsWith(outputRoot)) {
            throw new IllegalArgumentException("项目目录无效");
        }
        Files.createDirectories(projectRoot);
        rejectSymbolicLinks(projectRoot);
        Path target = projectRoot.resolve(relativePath).normalize();
        if (!target.startsWith(projectRoot)) {
            throw new IllegalArgumentException("文件路径不能越过项目目录");
        }
        return target;
    }

    private void createSafeDirectories(Path directory, Path projectRoot) throws IOException {
        if (!directory.startsWith(projectRoot)) {
            throw new IllegalArgumentException("父目录越界");
        }
        Path current = projectRoot;
        for (Path part : projectRoot.relativize(directory)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current)
                        || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalArgumentException("文件路径包含不安全的父目录");
                }
            } else {
                Files.createDirectory(current);
            }
        }
    }

    private void rejectSymbolicLinks(Path path) throws IOException {
        if (Files.isSymbolicLink(outputRoot)) {
            throw new IllegalArgumentException("代码输出目录不能是符号链接");
        }
        Path current = outputRoot;
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(outputRoot)) {
            throw new IllegalArgumentException("文件路径越界");
        }
        Path relative = outputRoot.relativize(normalized);
        for (Path part : relative) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("文件路径不能包含符号链接");
            }
        }
    }
}
