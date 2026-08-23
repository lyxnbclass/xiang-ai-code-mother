package com.xiang.xiangaicodemother.workflow.node;

import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.workflow.ai.CodeQualityCheckService;
import com.xiang.xiangaicodemother.workflow.model.QualityResult;
import com.xiang.xiangaicodemother.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/** 有界读取生成文件并执行 AI 质量检查。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeQualityCheckNode {
    static final int MAX_TOTAL_CHARS = 300_000;
    static final int MAX_FILE_CHARS = 80_000;
    private static final Set<String> EXTENSIONS = Set.of(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx");

    private final CodeQualityCheckService qualityCheckService;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.from(state);
            String code = readProject(context.getGeneratedCodeDir());
            QualityResult result;
            if (code.isBlank()) {
                result = QualityResult.builder().valid(false)
                        .errors(List.of("未找到可检查的代码文件"))
                        .suggestions(List.of("确认代码生成节点成功写入文件"))
                        .build();
            } else {
                try {
                    result = qualityCheckService.check(code);
                    if (result == null || result.getValid() == null) {
                        throw new IllegalStateException("质检结果为空");
                    }
                } catch (Exception e) {
                    log.warn("AI 质量检查不可用，跳过 AI 质检: {}", e.getMessage());
                    result = QualityResult.builder().valid(true).errors(List.of())
                            .suggestions(List.of("AI 质量检查暂不可用，已保留实际构建校验"))
                            .build();
                }
            }
            if (!result.passed()) {
                context.setQualityRetryCount(context.getQualityRetryCount() + 1);
            }
            context.setQualityResult(result);
            context.setCurrentStep(result.passed() ? "代码质量检查通过" : "代码质量检查未通过");
            return WorkflowContext.save(context);
        });
    }

    static String readProject(String directory) {
        if (directory == null || directory.isBlank()) {
            return "";
        }
        Path allowedRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        Path root = Path.of(directory).toAbsolutePath().normalize();
        if (!root.startsWith(allowedRoot) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(root)) {
            return "";
        }
        StringBuilder content = new StringBuilder("# 项目文件与代码\n\n");
        try (var paths = Files.walk(root)) {
            paths.filter(path -> isSafeCodeFile(root, path))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> appendFile(root, path, content));
        } catch (IOException e) {
            log.warn("读取质检文件失败: {}", e.getMessage());
        }
        return content.toString();
    }

    private static boolean isSafeCodeFile(Path root, Path path) {
        if (contentLimitReached(path, root) || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path relative = root.relativize(path);
        for (Path part : relative) {
            String name = part.toString();
            if (name.startsWith(".") || Set.of("node_modules", "dist", "target").contains(name)) {
                return false;
            }
        }
        String lower = path.getFileName().toString().toLowerCase();
        return EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private static boolean contentLimitReached(Path path, Path root) {
        return !path.normalize().startsWith(root);
    }

    private static void appendFile(Path root, Path path, StringBuilder content) {
        if (content.length() >= MAX_TOTAL_CHARS) {
            return;
        }
        try {
            long size = Files.size(path);
            if (size > MAX_FILE_CHARS * 4L) {
                return;
            }
            String value = Files.readString(path, StandardCharsets.UTF_8);
            int available = Math.min(MAX_FILE_CHARS, MAX_TOTAL_CHARS - content.length());
            if (available <= 0) {
                return;
            }
            String clipped = value.length() > available ? value.substring(0, available) : value;
            content.append("## 文件: ").append(root.relativize(path).toString().replace('\\', '/'))
                    .append("\n\n").append(clipped).append("\n\n");
        } catch (Exception e) {
            log.debug("跳过无法读取的质检文件 {}: {}", path.getFileName(), e.getMessage());
        }
    }
}
