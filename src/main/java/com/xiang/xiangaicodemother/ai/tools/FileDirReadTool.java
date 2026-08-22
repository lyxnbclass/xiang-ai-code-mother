package com.xiang.xiangaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** AI 读取 Vue 工程目录结构的受限工具。 */
@Component
@Slf4j
public class FileDirReadTool extends BaseTool {

    private static final int MAX_ENTRIES = 500;
    private static final int MAX_DEPTH = 12;
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", "target", ".mvn",
            ".idea", ".vscode", "coverage", ".ds_store"
    );
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache"
    );

    public FileDirReadTool() {
        super();
    }

    FileDirReadTool(Path outputRoot) {
        super(outputRoot);
    }

    @Tool("读取当前 Vue 工程的目录结构；路径为空时读取整个项目")
    public String readDir(@P("项目内目录的相对路径，可以为空") String relativeDirPath,
                          @ToolMemoryId Long appId) {
        try {
            Path projectRoot = resolveProjectRoot(appId, false);
            Path target = resolveProjectPath(relativeDirPath, appId, true, false);
            if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("目录不存在或不是普通目录");
            }
            List<String> entries = new ArrayList<>();
            boolean[] truncated = {false};
            Files.walkFileTree(target, Set.of(), MAX_DEPTH, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(target) && (Files.isSymbolicLink(dir) || shouldIgnore(dir.getFileName().toString()))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!dir.equals(target) && !addEntry(entries,
                            projectRoot.relativize(dir).toString().replace('\\', '/') + "/")) {
                        truncated[0] = true;
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isRegularFile() || Files.isSymbolicLink(file)
                            || shouldIgnore(file.getFileName().toString())) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!addEntry(entries, projectRoot.relativize(file).toString().replace('\\', '/'))) {
                        truncated[0] = true;
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            entries.sort(Comparator.naturalOrder());
            StringBuilder result = new StringBuilder("项目目录结构:\n");
            entries.forEach(entry -> result.append("- ").append(entry).append('\n'));
            if (truncated[0]) {
                result.append("- ...（目录条目超过 ").append(MAX_ENTRIES).append("，已截断）\n");
            }
            log.info("Vue 工程目录读取成功，appId={}，path={}，entries={}",
                    appId, relativeDirPath, entries.size());
            return result.toString();
        } catch (Exception e) {
            log.warn("Vue 工程目录读取被拒绝，appId={}，path={}，reason={}",
                    appId, relativeDirPath, e.getMessage());
            return failure("目录读取", e);
        }
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "读取目录";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("[工具调用] %s %s", getDisplayName(),
                displayPath(arguments.getStr("relativeDirPath")));
    }

    private boolean shouldIgnore(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        if (IGNORED_NAMES.contains(normalized) || normalized.startsWith(".env")) {
            return true;
        }
        return IGNORED_EXTENSIONS.stream().anyMatch(normalized::endsWith);
    }

    private boolean addEntry(List<String> entries, String entry) {
        if (entries.size() >= MAX_ENTRIES) {
            return false;
        }
        entries.add(entry);
        return true;
    }
}
