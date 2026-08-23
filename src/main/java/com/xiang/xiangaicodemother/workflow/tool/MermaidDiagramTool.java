package com.xiang.xiangaicodemother.workflow.tool;

import cn.hutool.core.util.StrUtil;
import com.xiang.xiangaicodemother.storage.ObjectStorageService;
import com.xiang.xiangaicodemother.workflow.model.ImageCategoryEnum;
import com.xiang.xiangaicodemother.workflow.model.ImageResource;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 通过 Mermaid CLI 安全生成 SVG 并上传对象存储。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MermaidDiagramTool {
    private static final int MAX_MERMAID_LENGTH = 20_000;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private final ObjectStorageService objectStorageService;

    @Tool("将 Mermaid 代码转换为架构图图片")
    public List<ImageResource> generate(@P("Mermaid 图表代码") String mermaidCode,
                                        @P("架构图描述") String description) {
        if (!objectStorageService.isAvailable() || StrUtil.isBlank(mermaidCode)
                || mermaidCode.length() > MAX_MERMAID_LENGTH) {
            return List.of();
        }
        Path input = null;
        Path output = null;
        try {
            input = Files.createTempFile("workflow-mermaid-", ".mmd");
            output = Files.createTempFile("workflow-mermaid-", ".svg");
            Files.writeString(input, mermaidCode, StandardCharsets.UTF_8);
            String executable = System.getProperty("os.name", "").toLowerCase().contains("windows")
                    ? "mmdc.cmd" : "mmdc";
            Process process = new ProcessBuilder(executable, "-i", input.toString(), "-o", output.toString(),
                    "-b", "transparent")
                    .redirectErrorStream(true)
                    .start();
            Thread.ofVirtual().start(() -> drain(process.getInputStream()));
            if (!process.waitFor(TIMEOUT.toSeconds(), TimeUnit.SECONDS) || process.exitValue() != 0
                    || Files.size(output) == 0) {
                process.destroyForcibly();
                return List.of();
            }
            String key = "workflow/mermaid/" + UUID.randomUUID() + ".svg";
            String url = objectStorageService.upload(key, output.toFile());
            if (!ContentImageSearchTool.isHttpUrl(url)) {
                return List.of();
            }
            return List.of(ImageResource.builder().category(ImageCategoryEnum.ARCHITECTURE)
                    .description(StrUtil.blankToDefault(description, "架构图")).url(url).build());
        } catch (Exception e) {
            log.warn("Mermaid 架构图生成失败: {}", e.getMessage());
            return List.of();
        } finally {
            deleteQuietly(input);
            deleteQuietly(output);
        }
    }

    private static void drain(InputStream input) {
        try (input; OutputStream sink = OutputStream.nullOutputStream()) {
            input.transferTo(sink);
        } catch (Exception ignored) {
        }
    }

    private static void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {
            }
        }
    }
}
