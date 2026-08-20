package com.xiang.xiangaicodemother.controller;

import com.xiang.xiangaicodemother.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 提供生成结果的本地预览。
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    private static final Path PREVIEW_ROOT = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR)
            .toAbsolutePath().normalize();

    @GetMapping("/{directory}/**")
    public ResponseEntity<Resource> serveStaticResource(@PathVariable String directory,
                                                         HttpServletRequest request) {
        if (!directory.matches("[A-Za-z0-9_-]+")) {
            return ResponseEntity.badRequest().build();
        }

        String mappingPath = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (mappingPath == null) {
            return ResponseEntity.notFound().build();
        }

        String prefix = "/static/" + directory;
        String resourcePath = mappingPath.startsWith(prefix)
                ? mappingPath.substring(prefix.length())
                : "";
        if (resourcePath.isEmpty()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(java.net.URI.create(request.getRequestURI() + "/"));
            return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
        }

        String relativePath = "/".equals(resourcePath)
                ? "index.html"
                : resourcePath.replaceFirst("^/+", "");
        Path directoryRoot = PREVIEW_ROOT.resolve(directory).normalize();
        Path file = directoryRoot.resolve(relativePath).normalize();
        if (!directoryRoot.startsWith(PREVIEW_ROOT) || !file.startsWith(directoryRoot)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(resolveContentType(file))
                .body(new FileSystemResource(file));
    }

    private MediaType resolveContentType(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".html")) {
            return MediaType.parseMediaType("text/html;charset=UTF-8");
        }
        if (fileName.endsWith(".css")) {
            return MediaType.parseMediaType("text/css;charset=UTF-8");
        }
        if (fileName.endsWith(".js")) {
            return MediaType.parseMediaType("application/javascript;charset=UTF-8");
        }
        try {
            String contentType = Files.probeContentType(file);
            if (contentType != null) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // 使用二进制类型兜底。
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
