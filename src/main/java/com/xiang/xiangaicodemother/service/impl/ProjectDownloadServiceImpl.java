package com.xiang.xiangaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiang.xiangaicodemother.constant.AppConstant;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import com.xiang.xiangaicodemother.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 安全地流式打包生成目录，避免临时 ZIP 和符号链接越界。
 */
@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".ds_store", ".env",
            "target", ".mvn", ".idea", ".vscode"
    );

    private static final Set<String> IGNORED_EXTENSIONS = Set.of(".log", ".tmp", ".cache");

    @Override
    public void downloadProjectAsZip(Path projectRoot, String downloadFileName,
                                     HttpServletResponse response) {
        if (StrUtil.isBlank(downloadFileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "下载文件名不能为空");
        }
        String safeFileName = downloadFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!safeFileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            safeFileName += ".zip";
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(safeFileName, StandardCharsets.UTF_8)
                .build()
                .toString());
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        try {
            writeProjectZip(projectRoot, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("输出项目 ZIP 失败，path={}", projectRoot, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目代码下载失败");
        }
    }

    @Override
    public void writeProjectZip(Path projectRoot, OutputStream outputStream) {
        Path root = validateProjectRoot(projectRoot);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) {
                    if (!directory.equals(root) && (attrs.isSymbolicLink() || isIgnored(root, directory))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isRegularFile() || attrs.isSymbolicLink() || isIgnored(root, file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String entryName = root.relativize(file).toString().replace('\\', '/');
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zipOutputStream);
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            zipOutputStream.finish();
        } catch (IOException e) {
            log.error("打包项目源码失败，path={}", root, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打包项目源码失败");
        }
    }

    private Path validateProjectRoot(Path projectRoot) {
        if (projectRoot == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "项目目录不能为空");
        }
        try {
            Path outputRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize()
                    .toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path realProjectRoot = projectRoot.toAbsolutePath().normalize()
                    .toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realProjectRoot.startsWith(outputRoot)
                    || !Files.isDirectory(realProjectRoot, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(realProjectRoot)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "项目目录不在允许的生成目录中");
            }
            return realProjectRoot;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成应用");
        }
    }

    private boolean isIgnored(Path root, Path path) {
        Path relativePath = root.relativize(path);
        for (Path part : relativePath) {
            String name = part.toString().toLowerCase(Locale.ROOT);
            if (IGNORED_NAMES.contains(name) || name.startsWith(".env.")) {
                return true;
            }
            for (String extension : IGNORED_EXTENSIONS) {
                if (name.endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }
}
