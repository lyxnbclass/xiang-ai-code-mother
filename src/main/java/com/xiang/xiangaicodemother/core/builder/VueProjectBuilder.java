package com.xiang.xiangaicodemother.core.builder;

import com.xiang.xiangaicodemother.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/** 安装依赖并构建 AI 生成的 Vue 工程。 */
@Component
@Slf4j
public class VueProjectBuilder {

    private static final Duration INSTALL_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(3);

    private final Path allowedRoot;
    private final Map<Path, ReentrantLock> projectLocks = new ConcurrentHashMap<>();

    public VueProjectBuilder() {
        this(Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR));
    }

    VueProjectBuilder(Path allowedRoot) {
        this.allowedRoot = allowedRoot.toAbsolutePath().normalize();
    }

    public CompletableFuture<Boolean> buildProjectAsync(String projectPath) {
        return CompletableFuture.supplyAsync(() -> buildProject(projectPath), command ->
                Thread.ofVirtual().name("vue-builder-" + System.nanoTime()).start(command));
    }

    public boolean buildProject(String projectPath) {
        Path projectDir;
        try {
            projectDir = validateProjectPath(projectPath);
        } catch (Exception e) {
            log.error("Vue 项目校验失败: {}", e.getMessage());
            return false;
        }

        ReentrantLock lock = projectLocks.computeIfAbsent(projectDir, ignored -> new ReentrantLock());
        lock.lock();
        try {
            log.info("开始构建 Vue 项目: {}", projectDir.getFileName());
            if (!executeNpmInstall(projectDir) || !executeNpmBuild(projectDir)) {
                return false;
            }
            Path distDir = projectDir.resolve("dist");
            Path indexFile = distDir.resolve("index.html");
            boolean validOutput = Files.isDirectory(distDir, LinkOption.NOFOLLOW_LINKS)
                    && Files.isRegularFile(indexFile, LinkOption.NOFOLLOW_LINKS);
            if (!validOutput) {
                log.error("Vue 构建未生成 dist/index.html: {}", projectDir.getFileName());
                return false;
            }
            log.info("Vue 项目构建成功: {}", projectDir.getFileName());
            return true;
        } finally {
            lock.unlock();
        }
    }

    protected boolean executeNpmInstall(Path projectDir) {
        return executeCommand(projectDir,
                List.of(npmExecutable(), "install", "--no-audit", "--no-fund"), INSTALL_TIMEOUT);
    }

    protected boolean executeNpmBuild(Path projectDir) {
        return executeCommand(projectDir, List.of(npmExecutable(), "run", "build"), BUILD_TIMEOUT);
    }

    private Path validateProjectPath(String projectPath) throws IOException {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("项目路径不能为空");
        }
        Path projectDir = Path.of(projectPath).toAbsolutePath().normalize();
        if (!projectDir.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("项目路径不在代码输出目录内");
        }
        if (!Files.isDirectory(projectDir, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(projectDir)) {
            throw new IllegalArgumentException("项目目录不存在或不安全");
        }
        Path packageJson = projectDir.resolve("package.json");
        if (!Files.isRegularFile(packageJson, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(packageJson)) {
            throw new IllegalArgumentException("项目缺少安全的 package.json");
        }
        return projectDir;
    }

    private boolean executeCommand(Path workingDir, List<String> command, Duration timeout) {
        Process process = null;
        try {
            log.info("在 {} 执行命令: {}", workingDir.getFileName(), String.join(" ", command));
            process = new ProcessBuilder(command)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            Process runningProcess = process;
            Thread outputDrainer = Thread.ofVirtual().name("vue-build-output-" + System.nanoTime())
                    .start(() -> drain(runningProcess.getInputStream()));
            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                log.error("Vue 构建命令超时: {}", command.get(0));
                return false;
            }
            outputDrainer.join(TimeUnit.SECONDS.toMillis(10));
            if (process.exitValue() != 0) {
                log.error("Vue 构建命令失败，退出码={}", process.exitValue());
                return false;
            }
            return true;
        } catch (Exception e) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            log.error("Vue 构建命令执行异常: {}", e.getMessage());
            return false;
        }
    }

    private void drain(InputStream inputStream) {
        try (inputStream; OutputStream sink = OutputStream.nullOutputStream()) {
            inputStream.transferTo(sink);
        } catch (IOException e) {
            log.debug("读取 Vue 构建输出结束: {}", e.getMessage());
        }
    }

    private String npmExecutable() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows") ? "npm.cmd" : "npm";
    }
}
