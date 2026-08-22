package com.xiang.xiangaicodemother.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.xiang.xiangaicodemother.constant.AppConstant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Vue 工程文件工具基类，统一提供展示信息和项目目录安全校验。
 */
public abstract class BaseTool {

    protected static final int MAX_PATH_LENGTH = 240;
    protected static final int MAX_CONTENT_BYTES = 1_048_576;
    private static final int MAX_DISPLAY_CONTENT_LENGTH = 4_000;

    private final Path outputRoot;

    protected BaseTool() {
        this(Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR));
    }

    protected BaseTool(Path outputRoot) {
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
    }

    public abstract String getToolName();

    public abstract String getDisplayName();

    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    public abstract String generateToolExecutedResult(JSONObject arguments);

    protected Path resolveProjectPath(String relativePath, Long appId,
                                      boolean allowProjectRoot, boolean createProject) throws IOException {
        Path projectRoot = resolveProjectRoot(appId, createProject);
        if (StrUtil.isBlank(relativePath)) {
            if (allowProjectRoot) {
                return projectRoot;
            }
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (relativePath.length() > MAX_PATH_LENGTH || relativePath.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("文件路径为空或过长");
        }

        final Path normalizedRelativePath;
        try {
            Path candidate = Path.of(relativePath);
            if (candidate.isAbsolute() || relativePath.matches("^[A-Za-z]:.*")) {
                throw new IllegalArgumentException("只允许项目内的相对路径");
            }
            normalizedRelativePath = candidate.normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("文件路径格式无效");
        }
        if (normalizedRelativePath.getNameCount() == 0 || normalizedRelativePath.startsWith("..")) {
            throw new IllegalArgumentException("文件路径不能越过项目目录");
        }

        Path target = projectRoot.resolve(normalizedRelativePath).normalize();
        if (!target.startsWith(projectRoot)) {
            throw new IllegalArgumentException("文件路径不能越过项目目录");
        }
        rejectSymbolicLinks(projectRoot, target);
        return target;
    }

    protected Path resolveProjectRoot(Long appId, boolean createProject) throws IOException {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("应用 ID 无效");
        }
        if (Files.exists(outputRoot, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(outputRoot)) {
            throw new IllegalArgumentException("代码输出目录不能是符号链接");
        }
        if (createProject) {
            Files.createDirectories(outputRoot);
        } else if (!Files.isDirectory(outputRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("代码输出目录不存在");
        }

        Path projectRoot = outputRoot.resolve("vue_project_" + appId).normalize();
        if (!projectRoot.startsWith(outputRoot)) {
            throw new IllegalArgumentException("项目目录无效");
        }
        if (Files.exists(projectRoot, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(projectRoot)) {
            throw new IllegalArgumentException("项目目录不能是符号链接");
        }
        if (createProject) {
            Files.createDirectories(projectRoot);
        } else if (!Files.isDirectory(projectRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Vue 工程不存在");
        }
        rejectSymbolicLinks(outputRoot, projectRoot);
        return projectRoot;
    }

    protected void createSafeParentDirectories(Path target, Path projectRoot) throws IOException {
        Path parent = target.getParent();
        if (parent == null || !parent.startsWith(projectRoot)) {
            throw new IllegalArgumentException("父目录越界");
        }
        Path current = projectRoot;
        for (Path part : projectRoot.relativize(parent)) {
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

    protected void validateContent(String content, String fieldName) {
        if (content == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException(fieldName + "超过 1 MB");
        }
    }

    protected String displayPath(String relativePath) {
        if (StrUtil.isBlank(relativePath)) {
            return "根目录";
        }
        try {
            return Path.of(relativePath).normalize().toString().replace('\\', '/');
        } catch (InvalidPathException e) {
            return "无效路径";
        }
    }

    protected String displayContent(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_DISPLAY_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_DISPLAY_CONTENT_LENGTH)
                + "\n...（内容过长，已截断展示）";
    }

    protected String failure(String action, Exception e) {
        return action + "失败: " + StrUtil.blankToDefault(e.getMessage(), "未知错误");
    }

    private void rejectSymbolicLinks(Path root, Path target) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("文件路径越界");
        }
        Path current = normalizedRoot;
        if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
            throw new IllegalArgumentException("文件路径不能包含符号链接");
        }
        for (Path part : normalizedRoot.relativize(normalizedTarget)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("文件路径不能包含符号链接");
            }
        }
    }
}
